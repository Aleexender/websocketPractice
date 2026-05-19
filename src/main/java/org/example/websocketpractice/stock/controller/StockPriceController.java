package org.example.websocketpractice.stock.controller;

import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.stock.dto.StockPriceResponse;
import org.example.websocketpractice.stock.query.StockPriceQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockPriceController {

    private final StockPriceQuery stockPriceQuery;

    public StockPriceController(StockPriceQuery stockPriceQuery) {
        this.stockPriceQuery = stockPriceQuery;
    }

    @GetMapping("/{symbol}/price")
    public StockPriceResponse.Single currentPrice(@PathVariable String symbol) {
        StockPrice price = stockPriceQuery.currentPrice(symbol);
        return new StockPriceResponse.Single(
                price.symbol(),
                price.priceInWon(),
                price.fetchedAt().toEpochMilli()
        );
    }
}
