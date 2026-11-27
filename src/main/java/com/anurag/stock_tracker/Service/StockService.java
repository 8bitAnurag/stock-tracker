package com.anurag.stock_tracker.Service;

import com.anurag.stock_tracker.Client.StockClient;
import com.anurag.stock_tracker.DTO.*;
import com.anurag.stock_tracker.Entity.FavouriteStock;
import com.anurag.stock_tracker.Exception.FavAlreadyExistsException;
import com.anurag.stock_tracker.Exception.FavDoesNotExistsException;
import com.anurag.stock_tracker.Repository.FavouriteStockRepository;
import com.anurag.stock_tracker.common.MarketCapFormat;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final FavouriteStockRepository favouriteStockRepository;
    private final StockClient stockClient;

    public StockService(final StockClient stockClient, final FavouriteStockRepository favouriteStockRepository) {
        this.stockClient = stockClient;
        this.favouriteStockRepository = favouriteStockRepository;
    }

    @Cacheable(value = "stock", key = "#stockSymbol", unless = "#result.price() == 0")
    public StockResponse getStockForSymbol(final String stockSymbol) {
        final AlphaVantageResponse response = stockClient.getStockQuote(stockSymbol);

        if (response != null && response.globalQuote() != null && response.globalQuote().price() != null) {
            return StockResponse.builder()
                    .symbol(response.globalQuote().symbol())
                    .price(Double.parseDouble(response.globalQuote().price()))
                    .lastUpdates(response.globalQuote().lastTradingDay())
                    .build();
        }

        JsonNode yahoo = stockClient.getYahooChart(stockSymbol, "5d");
        if (yahoo != null) {
            JsonNode meta = yahoo.path("meta");
            double price = meta.path("regularMarketPrice").asDouble(0.0);
            long marketTime = meta.path("regularMarketTime").asLong(0L);
            String lastDate = marketTime > 0
                    ? Instant.ofEpochSecond(marketTime).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    : "N/A";
            if (price > 0) {
                return StockResponse.builder()
                        .symbol(meta.path("symbol").asText(stockSymbol))
                        .price(round2(price))
                        .lastUpdates(lastDate)
                        .build();
            }
        }

        return StockResponse.builder()
                .symbol(stockSymbol)
                .price(0)
                .lastUpdates("N/A")
                .build();
    }

    @Cacheable(value = "overview", key = "#stockSymbol", unless = "#result.sector().equals('N/A')")
    public StockOverviewResponse getStockOverviewForSymbol(final String stockSymbol) {
        StockOverviewResponse response = stockClient.getStockOverview(stockSymbol);

        if (response != null && response.symbol() != null) {
            String formattedMarketCap = MarketCapFormat.format(response.marketCap());
            return new StockOverviewResponse(
                    response.symbol(),
                    response.Name(),
                    response.description(),
                    response.sector(),
                    response.industry(),
                    formattedMarketCap,
                    response.peRatio(),
                    response.dividendYield()
            );
        }

        // Fast fundamentals via TradingView scanner (Market Cap, P/E, Div Yield, Sector, Industry)
        JsonNode tv = stockClient.getTradingViewFundamentals(stockSymbol);
        if (tv != null && tv.path("d").isArray() && tv.path("d").size() >= 7) {
            JsonNode d = tv.path("d");
            String exchangeFull = tv.path("s").asText("");
            String exchange = exchangeFull.contains(":") ? exchangeFull.split(":")[0] : "US";
            String name = d.get(1).asText(stockSymbol);
            String sector = d.get(2).isNull() ? "N/A" : d.get(2).asText("N/A");
            String industry = d.get(3).isNull() ? "N/A" : d.get(3).asText("N/A");

            String marketCapFormatted = "N/A";
            if (!d.get(4).isNull() && d.get(4).isNumber()) {
                marketCapFormatted = MarketCapFormat.format(
                        BigDecimal.valueOf(d.get(4).asDouble()).toPlainString()
                );
            }

            String peFormatted = !d.get(5).isNull() && d.get(5).isNumber()
                    ? String.format(Locale.US, "%.2f", d.get(5).asDouble())
                    : "N/A";

            String divFormatted = !d.get(6).isNull() && d.get(6).isNumber()
                    ? String.format(Locale.US, "%.2f%%", d.get(6).asDouble())
                    : "0.00%";

            String description = String.format(
                    "%s (%s) trades on %s in the %s sector (%s).",
                    name, stockSymbol, exchange, sector, industry
            );

            return new StockOverviewResponse(
                    stockSymbol,
                    name,
                    description,
                    sector,
                    industry,
                    marketCapFormatted,
                    peFormatted,
                    divFormatted
            );
        }

        JsonNode yahoo = stockClient.getYahooChart(stockSymbol, "5d");
        if (yahoo != null) {
            JsonNode meta = yahoo.path("meta");
            String name = meta.path("longName").asText(meta.path("shortName").asText(stockSymbol));
            String exchange = meta.path("fullExchangeName").asText(meta.path("exchangeName").asText("Equity"));
            String currency = meta.path("currency").asText("USD");
            return new StockOverviewResponse(
                    meta.path("symbol").asText(stockSymbol),
                    name,
                    String.format("%s (%s) trades on %s (%s).", name, stockSymbol, exchange, currency),
                    exchange,
                    currency + " / " + meta.path("instrumentType").asText("EQUITY"),
                    "N/A",
                    "N/A",
                    "N/A"
            );
        }

        return new StockOverviewResponse(
                stockSymbol,
                stockSymbol,
                "Overview temporarily unavailable for this ticker.",
                "N/A",
                "N/A",
                "N/A",
                "N/A",
                "N/A"
        );
    }

    @Cacheable(value = "history", key = "#symbol + '-' + #days", unless = "#result.isEmpty()")
    public List<DailyStockResponse> getHistory(String symbol, int days) {
        StockHistoryResponse response = stockClient.getStockHistory(symbol);

        if (response != null && response.timeSeries() != null && !response.timeSeries().isEmpty()) {
            return response.timeSeries().entrySet().stream()
                    .limit(days)
                    .map(entry -> {
                        var date = entry.getKey();
                        var daily = entry.getValue();
                        return new DailyStockResponse(
                                date,
                                Double.parseDouble(daily.open()),
                                Double.parseDouble(daily.close()),
                                Double.parseDouble(daily.high()),
                                Double.parseDouble(daily.low()),
                                Long.parseLong(daily.volume())
                        );
                    })
                    .collect(Collectors.toList());
        }

        String range = days > 14 ? "3mo" : "1mo";
        JsonNode yahoo = stockClient.getYahooChart(symbol, range);
        if (yahoo == null) {
            return List.of();
        }

        JsonNode timestamps = yahoo.path("timestamp");
        JsonNode quote = yahoo.path("indicators").path("quote");
        if (!timestamps.isArray() || !quote.isArray() || quote.isEmpty()) {
            return List.of();
        }

        JsonNode q = quote.get(0);
        JsonNode opens = q.path("open");
        JsonNode closes = q.path("close");
        JsonNode highs = q.path("high");
        JsonNode lows = q.path("low");
        JsonNode volumes = q.path("volume");

        List<DailyStockResponse> list = new ArrayList<>();
        for (int i = timestamps.size() - 1; i >= 0 && list.size() < days; i--) {
            if (closes.get(i) == null || closes.get(i).isNull()) {
                continue;
            }
            long ts = timestamps.get(i).asLong();
            String date = Instant.ofEpochSecond(ts).atZone(ZoneOffset.UTC).toLocalDate().toString();
            list.add(new DailyStockResponse(
                    date,
                    round2(opens.path(i).asDouble()),
                    round2(closes.path(i).asDouble()),
                    round2(highs.path(i).asDouble()),
                    round2(lows.path(i).asDouble()),
                    volumes.path(i).asLong(0L)
            ));
        }
        return list;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Transactional
    public FavouriteStock addFavourite(String symbol) {
        symbol = symbol.replace("\"", "").trim();
        if (favouriteStockRepository.existsBySymbol(symbol)) {
            throw new FavAlreadyExistsException(symbol);
        }

        FavouriteStock fav = FavouriteStock.builder()
                .symbol(symbol)
                .build();

        return favouriteStockRepository.save(fav);
    }

    @Transactional
    public FavouriteStock removeFavourite(String symbol) {
        if (!favouriteStockRepository.existsBySymbol(symbol)) {
            throw new FavDoesNotExistsException(symbol);
        }
        FavouriteStock fav = favouriteStockRepository.findBySymbol(symbol);
        favouriteStockRepository.delete(fav);
        return fav;
    }

    public List<StockResponse> getFavWithPrices() {
        List<FavouriteStock> favourites = favouriteStockRepository.findAll();

        // Fetch watchlist prices concurrently when Alpha Vantage is bypassed/cached
        return favourites.parallelStream()
                .map(fav -> getStockForSymbol(fav.getSymbol()))
                .collect(Collectors.toList());
    }
}
