package org.example.websocketpractice.stock.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.example.websocketpractice.stock.exception.StockPriceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisAccessTokenProviderTest {

    private MockWebServer server;
    private KisAccessTokenProvider provider;

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
        provider = new KisAccessTokenProvider(restClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void accessToken_returnsTokenFromKis_whenFirstCalled() {
        // given
        server.enqueue(jsonResponse(
                "{\"access_token\":\"abc\",\"expires_in\":86400,\"token_type\":\"Bearer\"}"));

        // when
        String token = provider.accessToken();

        // then
        assertThat(token).isEqualTo("abc");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void accessToken_returnsCachedToken_whenCalledTwiceWithinExpiry() {
        // given
        server.enqueue(jsonResponse(
                "{\"access_token\":\"abc\",\"expires_in\":86400,\"token_type\":\"Bearer\"}"));

        // when
        provider.accessToken();
        String second = provider.accessToken();

        // then
        assertThat(second).isEqualTo("abc");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void accessToken_throwsUnavailable_whenKisRespondsWith500() {
        // given
        server.enqueue(new MockResponse().setResponseCode(500));

        // then
        assertThatThrownBy(() -> provider.accessToken())
                .isInstanceOf(StockPriceUnavailableException.class);
    }

    @Test
    void accessToken_throwsUnavailable_whenAccessTokenIsBlank() {
        // given
        server.enqueue(jsonResponse(
                "{\"access_token\":\"\",\"expires_in\":86400,\"token_type\":\"Bearer\"}"));

        // then
        assertThatThrownBy(() -> provider.accessToken())
                .isInstanceOf(StockPriceUnavailableException.class);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json");
    }
}
