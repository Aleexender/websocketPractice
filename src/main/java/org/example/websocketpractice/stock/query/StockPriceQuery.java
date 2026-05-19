package org.example.websocketpractice.stock.query;

import org.example.websocketpractice.stock.client.StockPriceClient;
import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.stock.exception.InvalidStockSymbolException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class StockPriceQuery {

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^\\d{6}$");

    private final StockPriceClient priceClient;

    public StockPriceQuery(StockPriceClient priceClient) {
        this.priceClient = priceClient;
    }

    public StockPrice currentPrice(String symbol) {
        validate(symbol);
        return priceClient.fetchCurrentPrice(symbol);
    }

    private void validate(String symbol) {
        if (symbol == null || !SYMBOL_PATTERN.matcher(symbol).matches()) {
            throw new InvalidStockSymbolException(symbol);
        }
    }
}
