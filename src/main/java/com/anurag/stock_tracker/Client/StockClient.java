package com.anurag.stock_tracker.Client;

import com.anurag.stock_tracker.DTO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class StockClient {

    private final WebClient webClient;
    private long lastCallTimeMs = 0L;

    @Value("${alpha.vantage.api.key}")
    private String apiKey;

    /**
     * Alpha Vantage's free tier enforces a strict 1 request/second burst limit.
     * Spacing outbound requests by 1.1s prevents concurrent calls from failing.
     */
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
    }

    public StockOverviewResponse getStockOverview(String symbol) {
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
    }

    public StockHistoryResponse getStockHistory(String symbol) {
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
    }
}
