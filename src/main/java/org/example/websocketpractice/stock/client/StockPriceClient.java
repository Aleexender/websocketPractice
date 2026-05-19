package org.example.websocketpractice.stock.client;

import org.example.websocketpractice.stock.domain.StockPrice;

public interface StockPriceClient {

    StockPrice fetchCurrentPrice(String symbol);
}
