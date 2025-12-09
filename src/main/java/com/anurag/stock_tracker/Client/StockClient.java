package com.anurag.stock_tracker.Client;

import com.anurag.stock_tracker.DTO.*;
import com.anurag.stock_tracker.Entity.FavouriteStock;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class StockClient {

    private final WebClient webClient;

    @Value("${alpha.vantage.api.key}")
    private String apiKey;

    public AlphaVantageResponse getStockQuote(String stockSymbol) { return webClient.get().uri(
                uriBuilder -> uriBuilder.path("/query")
                        .queryParam("function", "GLOBAL_QUOTE")
                        .queryParam("symbol", stockSymbol)
                        .queryParam("apikey", apiKey)
                        .build())
                        .retrieve()
                        .bodyToMono(AlphaVantageResponse.class).block(); }

    public StockOverviewResponse getStockOverview(String symbol){
        return webClient.get().uri(
                        uriBuilder -> uriBuilder.path("/query")
                                .queryParam("function", "OVERVIEW")
                                .queryParam("symbol", symbol)
                                .queryParam("apikey", apiKey)
                                .build())
                .retrieve()
                .bodyToMono(StockOverviewResponse.class).block();
    }

    public StockHistoryResponse getStockHistory(String symbol){
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
