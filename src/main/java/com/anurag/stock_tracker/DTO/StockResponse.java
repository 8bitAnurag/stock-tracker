package com.anurag.stock_tracker.DTO;

import lombok.Builder;

@Builder
public record StockResponse(
        String symbol,
        double price,
        String lastUpdates
) {
}
