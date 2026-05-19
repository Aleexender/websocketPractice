package org.example.websocketpractice.stock.query;

import org.example.websocketpractice.stock.client.StockPriceClient;
import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.stock.exception.InvalidStockSymbolException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockPriceQueryTest {

    @Test
    void currentPrice_returnsStockPrice_whenClientSucceeds() {
        // given
        StockPriceClient client = mock(StockPriceClient.class);
        StockPrice expected = new StockPrice("005930", 71500L, Instant.ofEpochMilli(1L));
        when(client.fetchCurrentPrice("005930")).thenReturn(expected);
        StockPriceQuery query = new StockPriceQuery(client);

        // when
        StockPrice actual = query.currentPrice("005930");

        // then
        assertThat(actual).isEqualTo(expected);
        verify(client).fetchCurrentPrice("005930");
    }

    @Test
    void currentPrice_throwsInvalidSymbol_whenSymbolIsNull() {
        // given
        StockPriceClient client = mock(StockPriceClient.class);
        StockPriceQuery query = new StockPriceQuery(client);

        // then
        assertThatThrownBy(() -> query.currentPrice(null))
                .isInstanceOf(InvalidStockSymbolException.class);
        verifyNoInteractions(client);
    }

    @Test
    void currentPrice_throwsInvalidSymbol_whenSymbolHasLetters() {
        // given
        StockPriceClient client = mock(StockPriceClient.class);
        StockPriceQuery query = new StockPriceQuery(client);

        // then
        assertThatThrownBy(() -> query.currentPrice("AAPL01"))
                .isInstanceOf(InvalidStockSymbolException.class);
        verifyNoInteractions(client);
    }

    @Test
    void currentPrice_throwsInvalidSymbol_whenSymbolIsNotSixDigits() {
        // given
        StockPriceClient client = mock(StockPriceClient.class);
        StockPriceQuery query = new StockPriceQuery(client);

        // then
        assertThatThrownBy(() -> query.currentPrice("12345"))
                .isInstanceOf(InvalidStockSymbolException.class);
        verifyNoInteractions(client);
    }
}
