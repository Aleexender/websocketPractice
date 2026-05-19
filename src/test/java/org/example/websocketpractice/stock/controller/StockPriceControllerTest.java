package org.example.websocketpractice.stock.controller;

import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.stock.exception.InvalidStockSymbolException;
import org.example.websocketpractice.stock.exception.StockExceptionHandler;
import org.example.websocketpractice.stock.exception.StockPriceUnavailableException;
import org.example.websocketpractice.stock.query.StockPriceQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockPriceController.class)
@Import(StockExceptionHandler.class)
class StockPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockPriceQuery stockPriceQuery;

    @Test
    void getCurrentPrice_returns200WithPriceBody_whenQuerySucceeds() throws Exception {
        // given
        when(stockPriceQuery.currentPrice("005930"))
                .thenReturn(new StockPrice("005930", 71500L, Instant.ofEpochMilli(1700000000000L)));

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/stocks/005930/price"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("005930"))
                .andExpect(jsonPath("$.priceInWon").value(71500))
                .andExpect(jsonPath("$.timestamp").value(1700000000000L));
    }

    @Test
    void getCurrentPrice_returns400_whenSymbolIsInvalid() throws Exception {
        // given
        when(stockPriceQuery.currentPrice("abc"))
                .thenThrow(new InvalidStockSymbolException("abc"));

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/stocks/abc/price"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SYMBOL"));
    }

    @Test
    void getCurrentPrice_returns503_whenKisIsUnavailable() throws Exception {
        // given
        when(stockPriceQuery.currentPrice("005930"))
                .thenThrow(new StockPriceUnavailableException("KIS down"));

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/stocks/005930/price"));

        // then
        result.andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PRICE_UNAVAILABLE"));
    }
}
