package com.anurag.stock_tracker.Controller;

import com.anurag.stock_tracker.DTO.AlphaVantageResponse;
import com.anurag.stock_tracker.DTO.DailyStockResponse;
import com.anurag.stock_tracker.DTO.StockOverviewResponse;
import com.anurag.stock_tracker.DTO.StockResponse;
import com.anurag.stock_tracker.Entity.FavouriteStock;
import com.anurag.stock_tracker.Service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {
    private final StockService stockService;

    @Autowired
    public StockController(final StockService stockService){
        this.stockService = stockService;
    }

    @GetMapping("/{stockSymbol}")
    public StockResponse getStock(@PathVariable String stockSymbol){
        return stockService.getStockForSymbol(stockSymbol.toUpperCase());
    }

    @GetMapping("/{stockSymbol}/overview")
    public StockOverviewResponse getStockOverview(@PathVariable String stockSymbol){
        return stockService.getStockOverviewForSymbol(stockSymbol.toUpperCase());
    }

    @GetMapping("/{stockSymbol}/history")
    public List<DailyStockResponse> getStockHistory(
            @PathVariable String stockSymbol,
            @RequestParam(defaultValue = "7") int days
    ){
        return stockService.getHistory(stockSymbol.toUpperCase(),days);
    }

    @PostMapping("/favourites")
    public  ResponseEntity<FavouriteStock> saveFavouriteStock(@RequestBody FavouriteStock request){
        final FavouriteStock saved = stockService.addFavourite(request.getSymbol());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/favourites")
    public List<StockResponse> getFavWithPrices(){
        return stockService.getFavWithPrices();
    }
}
