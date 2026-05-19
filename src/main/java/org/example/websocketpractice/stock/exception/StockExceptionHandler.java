package org.example.websocketpractice.stock.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "org.example.websocketpractice.stock")
public class StockExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(StockExceptionHandler.class);

    @ExceptionHandler(InvalidStockSymbolException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSymbol(InvalidStockSymbolException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_SYMBOL", e.getMessage()));
    }

    @ExceptionHandler(StockPriceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(StockPriceUnavailableException e) {
        log.warn("Stock price unavailable: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("PRICE_UNAVAILABLE", e.getMessage()));
    }

    public record ErrorResponse(String code, String message) {
    }
}
