package com.anurag.stock_tracker.Exception;

public class FavAlreadyExistsException extends RuntimeException{
    public FavAlreadyExistsException(String symbol) {
        super("Fav already exists for symbol: " + symbol);
    }
}
