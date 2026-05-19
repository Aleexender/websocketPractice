package org.example.websocketpractice.stock.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("token_type") String tokenType
) {
}
