package org.example.websocketpractice.stock.client;

import org.example.websocketpractice.stock.exception.StockPriceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class KisAccessTokenProvider {

    private static final Duration RENEWAL_MARGIN = Duration.ofMinutes(5);
    private static final String TOKEN_PATH = "/oauth2/tokenP";
    private static final String GRANT_TYPE = "client_credentials";

    private final RestClient kisRestClient;
    private final KisProperties properties;
    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public KisAccessTokenProvider(
            @Qualifier(KisClientConfig.KIS_REST_CLIENT) RestClient kisRestClient,
            KisProperties properties
    ) {
        this.kisRestClient = kisRestClient;
        this.properties = properties;
    }

    public String accessToken() {
        CachedToken current = cache.get();
        if (isValid(current)) {
            return current.token();
        }
        CachedToken issued = issueNew();
        cache.set(issued);
        return issued.token();
    }

    private boolean isValid(CachedToken token) {
        if (token == null) {
            return false;
        }
        return Instant.now().isBefore(token.expiresAt().minus(RENEWAL_MARGIN));
    }

    private CachedToken issueNew() {
        KisTokenResponse response = requestToken();
        ensureValid(response);
        Instant expiresAt = Instant.now().plusSeconds(response.expiresIn());
        return new CachedToken(response.accessToken(), expiresAt);
    }

    private KisTokenResponse requestToken() {
        try {
            return kisRestClient.post()
                    .uri(TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "grant_type", GRANT_TYPE,
                            "appkey", properties.appKey(),
                            "appsecret", properties.appSecret()
                    ))
                    .retrieve()
                    .body(KisTokenResponse.class);
        } catch (RestClientException e) {
            throw new StockPriceUnavailableException("Failed to issue KIS access token", e);
        }
    }

    private void ensureValid(KisTokenResponse response) {
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new StockPriceUnavailableException("Empty KIS token response");
        }
        if (response.expiresIn() <= 0) {
            throw new StockPriceUnavailableException("Invalid KIS token expiry: " + response.expiresIn());
        }
    }

    record CachedToken(String token, Instant expiresAt) {
    }
}
