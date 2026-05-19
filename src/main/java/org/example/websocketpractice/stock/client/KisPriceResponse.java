package org.example.websocketpractice.stock.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisPriceResponse(
        @JsonProperty("rt_cd") String returnCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output") Output output
) {

    public record Output(
            @JsonProperty("stck_prpr") String currentPrice
    ) {
    }
}
