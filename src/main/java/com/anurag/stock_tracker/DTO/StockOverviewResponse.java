package com.anurag.stock_tracker.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;


public record StockOverviewResponse(
        @JsonProperty("Symbol") String symbol,
        @JsonProperty("Name") String Name,
        @JsonProperty("Description") String description,
        @JsonProperty("Sector") String sector,
        @JsonProperty("Industry") String  industry,
        @JsonProperty("MarketCapitalization") String marketCap,
        @JsonProperty("PERatio") String peRatio ,
        @JsonProperty("DividendYield") String dividendYield

        ) {


}
