package org.example.websocketpractice.stock.domain;

import java.time.Instant;

public record StockPrice(
        String symbol,
        long priceInWon,
        Instant fetchedAt
) {
}
