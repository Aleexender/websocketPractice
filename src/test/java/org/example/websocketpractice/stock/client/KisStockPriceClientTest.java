package org.example.websocketpractice.stock.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.stock.exception.StockPriceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisStockPriceClientTest {

    private static final String TOKEN_RESPONSE =
            "{\"access_token\":\"abc\",\"expires_in\":86400,\"token_type\":\"Bearer\"}";

    private MockWebServer server;
    private KisStockPriceClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        KisProperties properties = new KisProperties(
                server.url("/").toString(),
                "app-key",
                "app-secret",
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );
        RestClient restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(restClient, properties);
        client = new KisStockPriceClient(restClient, tokenProvider, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void fetchCurrentPrice_returnsStockPrice_whenKisRespondsOk() {
        // given
        server.enqueue(jsonResponse(TOKEN_RESPONSE));
        server.enqueue(jsonResponse(
                "{\"rt_cd\":\"0\",\"msg1\":\"OK\",\"output\":{\"stck_prpr\":\"71500\"}}"));

        // when
        StockPrice price = client.fetchCurrentPrice("005930");

        // then
        assertThat(price.symbol()).isEqualTo("005930");
        assertThat(price.priceInWon()).isEqualTo(71500L);
    }

    @Test
    void fetchCurrentPrice_throwsUnavailable_whenKisReturnsErrorCode() {
        // given
        server.enqueue(jsonResponse(TOKEN_RESPONSE));
        server.enqueue(jsonResponse(
                "{\"rt_cd\":\"1\",\"msg1\":\"INVALID_SYMBOL\",\"output\":{\"stck_prpr\":\"\"}}"));

        // then
        assertThatThrownBy(() -> client.fetchCurrentPrice("005930"))
                .isInstanceOf(StockPriceUnavailableException.class);
    }

    @Test
    void fetchCurrentPrice_throwsUnavailable_whenKisRespondsWith500() {
        // given
        server.enqueue(jsonResponse(TOKEN_RESPONSE));
        server.enqueue(new MockResponse().setResponseCode(500));

        // then
        assertThatThrownBy(() -> client.fetchCurrentPrice("005930"))
                .isInstanceOf(StockPriceUnavailableException.class);
    }

    @Test
    void fetchCurrentPrice_throwsUnavailable_whenPriceFieldIsNotNumeric() {
        // given
        server.enqueue(jsonResponse(TOKEN_RESPONSE));
        server.enqueue(jsonResponse(
                "{\"rt_cd\":\"0\",\"msg1\":\"OK\",\"output\":{\"stck_prpr\":\"NaN\"}}"));

        // then
        assertThatThrownBy(() -> client.fetchCurrentPrice("005930"))
                .isInstanceOf(StockPriceUnavailableException.class);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json");
    }
}
