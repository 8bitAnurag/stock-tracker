package com.anurag.stock_tracker.Exception;

public class FavDoesNotExistsException extends RuntimeException {
    public FavDoesNotExistsException(String symbol) {
        super(symbol + " does not exist in favourites");
    }
}
