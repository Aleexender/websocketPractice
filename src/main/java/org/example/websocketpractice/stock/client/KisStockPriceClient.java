package org.example.websocketpractice.stock.client;

import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.stock.exception.StockPriceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

@Component
public class KisStockPriceClient implements StockPriceClient {

    private static final String PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String MARKET_DIV_DOMESTIC = "J";
    private static final String TR_ID_DOMESTIC_PRICE = "FHKST01010100";
    private static final String SUCCESS_CODE = "0";

    private final RestClient kisRestClient;
    private final KisAccessTokenProvider tokenProvider;
    private final KisProperties properties;

    public KisStockPriceClient(
            @Qualifier(KisClientConfig.KIS_REST_CLIENT) RestClient kisRestClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties
    ) {
        this.kisRestClient = kisRestClient;
        this.tokenProvider = tokenProvider;
        this.properties = properties;
    }

    @Override
    public StockPrice fetchCurrentPrice(String symbol) {
        KisPriceResponse response = call(symbol);
        ensureSuccess(response);
        long price = parsePrice(response, symbol);
        return new StockPrice(symbol, price, Instant.now());
    }

    private KisPriceResponse call(String symbol) {
        try {
            return kisRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PRICE_PATH)
                            .queryParam("FID_COND_MRKT_DIV_CODE", MARKET_DIV_DOMESTIC)
                            .queryParam("FID_INPUT_ISCD", symbol)
                            .build())
                    .header("authorization", "Bearer " + tokenProvider.accessToken())
                    .header("appkey", properties.appKey())
                    .header("appsecret", properties.appSecret())
                    .header("tr_id", TR_ID_DOMESTIC_PRICE)
                    .retrieve()
                    .body(KisPriceResponse.class);
        } catch (RestClientException e) {
            throw new StockPriceUnavailableException(
                    "KIS price request failed for symbol " + symbol, e);
        }
    }

    private void ensureSuccess(KisPriceResponse response) {
        if (response == null || response.output() == null) {
            throw new StockPriceUnavailableException("Empty KIS response");
        }
        if (!SUCCESS_CODE.equals(response.returnCode())) {
            throw new StockPriceUnavailableException("KIS returned error: " + response.message());
        }
    }

    private long parsePrice(KisPriceResponse response, String symbol) {
        String raw = response.output().currentPrice();
        if (raw == null || raw.isBlank()) {
            throw new StockPriceUnavailableException("Missing price for symbol " + symbol);
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new StockPriceUnavailableException(
                    "Invalid price format for " + symbol + ": " + raw, e);
        }
    }
}
