package com.anurag.stock_tracker.Client;

import com.anurag.stock_tracker.DTO.AlphaVantageResponse;
import com.anurag.stock_tracker.DTO.StockResponse;
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

    public AlphaVantageResponse getStockQuote(String stockSymbol)
        { return webClient.get().uri(
                uriBuilder -> uriBuilder.path("/query")
                        .queryParam("function", "GLOBAL_QUOTE")
                        .queryParam("symbol", stockSymbol)
                        .queryParam("apikey", apiKey)
                        .build())
                        .retrieve()
                        .bodyToMono(AlphaVantageResponse.class).block(); }
}
