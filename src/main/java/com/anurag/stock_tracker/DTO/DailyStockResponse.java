package com.anurag.stock_tracker.DTO;

public record DailyStockResponse(
        String date,
        double open,
        double close,
        double high,
        double low,
        long volume
) {


}
