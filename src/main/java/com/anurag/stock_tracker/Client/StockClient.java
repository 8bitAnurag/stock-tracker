package com.anurag.stock_tracker.Client;

import com.anurag.stock_tracker.DTO.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class StockClient {

    private final WebClient webClient;
    private final WebClient yahooClient = WebClient.builder()
            .baseUrl("https://query1.finance.yahoo.com")
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .build();

    private long lastCallTimeMs = 0L;

    @Value("${alpha.vantage.api.key}")
    private String apiKey;

    private synchronized void throttle() {
        long now = System.currentTimeMillis();
        long waitMs = 1150L - (now - lastCallTimeMs);
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTimeMs = System.currentTimeMillis();
    }

    public AlphaVantageResponse getStockQuote(String stockSymbol) {
        try {
            throttle();
            return webClient.get().uri(
                    uriBuilder -> uriBuilder.path("/query")
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", stockSymbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(AlphaVantageResponse.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    public StockOverviewResponse getStockOverview(String symbol) {
        try {
            throttle();
            return webClient.get().uri(
                    uriBuilder -> uriBuilder.path("/query")
                            .queryParam("function", "OVERVIEW")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(StockOverviewResponse.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    public StockHistoryResponse getStockHistory(String symbol) {
        try {
            throttle();
            return webClient.get().uri(
                    uriBuilder -> uriBuilder.path("/query")
                            .queryParam("function", "TIME_SERIES_DAILY")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(StockHistoryResponse.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Fallback market data source when Alpha Vantage rate-limits shared cloud IPs.
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
}
