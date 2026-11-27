package com.anurag.stock_tracker.Client;

import com.anurag.stock_tracker.DTO.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockClient {

    private static final long ALPHA_COOLDOWN_MS = 30 * 60 * 1000L; // 30 mins
    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private final WebClient webClient;
    private final WebClient yahooClient = WebClient.builder()
            .baseUrl("https://query1.finance.yahoo.com")
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .build();
    private final WebClient tradingViewClient = WebClient.builder()
            .baseUrl("https://scanner.tradingview.com")
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .build();

    private long lastCallTimeMs = 0L;
    private volatile long alphaCooldownUntilMs = 0L;

    @Value("${alpha.vantage.api.key}")
    private String apiKey;

    public boolean isAlphaVantageAvailable() {
        return apiKey != null
                && !apiKey.isBlank()
                && !"demo".equalsIgnoreCase(apiKey.trim())
                && System.currentTimeMillis() >= alphaCooldownUntilMs;
    }

    private void triggerAlphaCooldown() {
        alphaCooldownUntilMs = System.currentTimeMillis() + ALPHA_COOLDOWN_MS;
    }

    private synchronized boolean throttleAlpha() {
        if (!isAlphaVantageAvailable()) {
            return false;
        }
        long now = System.currentTimeMillis();
        long waitMs = 1150L - (now - lastCallTimeMs);
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        lastCallTimeMs = System.currentTimeMillis();
        return true;
    }

    public AlphaVantageResponse getStockQuote(String stockSymbol) {
        if (!throttleAlpha()) return null;
        try {
            AlphaVantageResponse resp = webClient.get().uri(
                    uriBuilder -> uriBuilder.path("/query")
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", stockSymbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(AlphaVantageResponse.class)
                    .timeout(TIMEOUT)
                    .block();
            if (resp == null || resp.globalQuote() == null || resp.globalQuote().price() == null) {
                triggerAlphaCooldown();
                return null;
            }
            return resp;
        } catch (Exception e) {
            triggerAlphaCooldown();
            return null;
        }
    }

    public StockOverviewResponse getStockOverview(String symbol) {
        if (!throttleAlpha()) return null;
        try {
            StockOverviewResponse resp = webClient.get().uri(
                    uriBuilder -> uriBuilder.path("/query")
                            .queryParam("function", "OVERVIEW")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(StockOverviewResponse.class)
                    .timeout(TIMEOUT)
                    .block();
            if (resp == null || resp.symbol() == null) {
                triggerAlphaCooldown();
                return null;
            }
            return resp;
        } catch (Exception e) {
            triggerAlphaCooldown();
            return null;
        }
    }

    public StockHistoryResponse getStockHistory(String symbol) {
        if (!throttleAlpha()) return null;
        try {
            StockHistoryResponse resp = webClient.get().uri(
                    uriBuilder -> uriBuilder.path("/query")
                            .queryParam("function", "TIME_SERIES_DAILY")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(StockHistoryResponse.class)
                    .timeout(TIMEOUT)
                    .block();
            if (resp == null || resp.timeSeries() == null || resp.timeSeries().isEmpty()) {
                triggerAlphaCooldown();
                return null;
            }
            return resp;
        } catch (Exception e) {
            triggerAlphaCooldown();
            return null;
        }
    }

    /**
     * Fast chart & price feed when Alpha Vantage is rate-limited.
     */
    public JsonNode getYahooChart(String symbol, String range) {
        try {
            JsonNode root = yahooClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v8/finance/chart/{symbol}")
                            .queryParam("range", range)
                            .queryParam("interval", "1d")
                            .build(symbol))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(TIMEOUT)
                    .block();
            if (root == null) return null;
            JsonNode results = root.path("chart").path("result");
            if (results.isArray() && !results.isEmpty()) {
                return results.get(0);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Fast fundamentals feed (Market Cap, P/E, Dividend Yield, Sector, Industry).
     */
    public JsonNode getTradingViewFundamentals(String symbol) {
        try {
            Map<String, Object> body = Map.of(
                    "filter", List.of(Map.of(
                            "left", "name",
                            "operation", "equal",
                            "right", symbol.toUpperCase()
                    )),
                    "sort", Map.of(
                            "sortBy", "market_cap_basic",
                            "sortOrder", "desc"
                    ),
                    "range", List.of(0, 1),
                    "columns", List.of(
                            "name",
                            "description",
                            "sector",
                            "industry",
                            "market_cap_basic",
                            "price_earnings_ttm",
                            "dividends_yield_current",
                            "close"
                    )
            );

            JsonNode root = tradingViewClient.post()
                    .uri("/america/scan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(TIMEOUT)
                    .block();

            if (root != null && root.path("data").isArray() && !root.path("data").isEmpty()) {
                return root.path("data").get(0);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
