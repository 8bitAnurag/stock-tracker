package com.anurag.stock_tracker.Service;

import com.anurag.stock_tracker.Client.StockClient;
import com.anurag.stock_tracker.DTO.*;
import com.anurag.stock_tracker.Entity.FavouriteStock;
import com.anurag.stock_tracker.Exception.FavAlreadyExistsException;
import com.anurag.stock_tracker.Repository.FavouriteStockRepository;
import com.anurag.stock_tracker.common.MarketCapFormat;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    private FavouriteStockRepository favouriteStockRepository;
    private StockClient stockClient;

    public StockService(final StockClient stockClient, final FavouriteStockRepository favouriteStockRepository)
    {
        this.stockClient = stockClient;
        this.favouriteStockRepository = favouriteStockRepository;
    }

    @Cacheable(value = "stock", key = "#stockSymbol")
    public StockResponse getStockForSymbol(final String stockSymbol) {
        final AlphaVantageResponse response = stockClient.getStockQuote(stockSymbol);

        if (response == null || response.globalQuote() == null) {
            System.out.println("NULL RESPONSE FOR: " + stockSymbol);

            return StockResponse.builder()
                    .symbol(stockSymbol)
                    .price(0)
                    .lastUpdates("N/A")
                    .build();
        }

        return StockResponse.builder()
                .symbol(response.globalQuote().symbol())
                .price(Double.parseDouble(response.globalQuote().price()))
                .lastUpdates(response.globalQuote().lastTradingDay())
                .build();
    }



    public StockOverviewResponse getStockOverviewForSymbol(final String stockSymbol) {
        StockOverviewResponse response = stockClient.getStockOverview(stockSymbol);

        String formattedMarketCap = MarketCapFormat.format(response.marketCap());

        return new StockOverviewResponse(
                response.symbol(),
                response.Name(),
                response.description(),
                response.sector(),
                response.industry(),// raw
                formattedMarketCap,            // formatted
                response.peRatio(),
                response.dividendYield()
        );
    }

    public List<DailyStockResponse> getHistory(String symbol, int days) {
        StockHistoryResponse response = stockClient.getStockHistory(symbol);

        if (response == null || response.timeSeries() == null) {
            System.out.println("NULL HISTORY FOR: " + symbol);
            return List.of(); // empty list instead of crashing
        }


        return response.timeSeries().entrySet().stream()
               .limit(days)
               .map( entry->{
                   var date = entry.getKey();
                   var daily = entry.getValue();
                   return new DailyStockResponse(
                           date,
                           Double.parseDouble(daily.open()),
                           Double.parseDouble(daily.high()),
                           Double.parseDouble(daily.low()),
                           Double.parseDouble(daily.close()),
                           Long.parseLong(daily.volume())
                   );
               })
               .collect(Collectors.toList());
    }

    @Transactional
    public FavouriteStock addFavourite(String symbol) {
        if (favouriteStockRepository.existsBySymbol(symbol)) {
            throw new FavAlreadyExistsException(symbol);
        }

        FavouriteStock fav = FavouriteStock.builder()
                .symbol(symbol)
                .build();

        return favouriteStockRepository.save(fav);
    }

    public List<StockResponse> getFavWithPrices(){
        List<FavouriteStock> favourites = favouriteStockRepository.findAll();

        return favourites.stream()
                .map(fav -> {
                    System.out.println("Fetching: " + fav.getSymbol());   // ← ADD THIS
                    return getStockForSymbol(fav.getSymbol());
                })
                .collect(Collectors.toList());
    }

}
