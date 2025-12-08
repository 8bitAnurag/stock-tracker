package com.anurag.stock_tracker.Service;

import com.anurag.stock_tracker.Client.StockClient;
import com.anurag.stock_tracker.DTO.StockResponse;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private StockClient stockClient;

    public StockService(final StockClient stockClient){
        this.stockClient = stockClient;
    }

    public StockResponse getStockForSymbol(final String stockSymbol) {
        return stockClient.getStockQuote(stockSymbol);
    }
}
