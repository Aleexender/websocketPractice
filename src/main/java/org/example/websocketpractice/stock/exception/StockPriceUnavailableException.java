package org.example.websocketpractice.stock.exception;

public class StockPriceUnavailableException extends RuntimeException {

    public StockPriceUnavailableException(String message) {
        super(message);
    }

    public StockPriceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
