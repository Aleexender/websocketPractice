package org.example.websocketpractice.stock.dto;

public sealed interface StockPriceResponse permits StockPriceResponse.Single {

    record Single(
            String symbol,
            long priceInWon,
            long timestamp
    ) implements StockPriceResponse {
    }
}
