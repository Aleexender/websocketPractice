package org.example.websocketpractice.stock.exception;

public class InvalidStockSymbolException extends RuntimeException {

    public InvalidStockSymbolException(String symbol) {
        super("Invalid stock symbol: " + symbol);
    }
}
