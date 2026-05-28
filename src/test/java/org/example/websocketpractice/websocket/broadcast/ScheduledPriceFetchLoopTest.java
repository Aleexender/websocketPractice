package org.example.websocketpractice.websocket.broadcast;

import org.example.websocketpractice.stock.domain.StockPrice;
import org.example.websocketpractice.stock.exception.StockPriceUnavailableException;
import org.example.websocketpractice.stock.query.StockPriceQuery;
import org.example.websocketpractice.websocket.protocol.ServerEvent;
import org.example.websocketpractice.websocket.subscription.SubscriptionRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScheduledPriceFetchLoopTest {

    @Test
    void tick_skipsFetch_whenNoActiveSymbols() {
        // given
        SubscriptionRegistry registry = mock(SubscriptionRegistry.class);
        StockPriceQuery query = mock(StockPriceQuery.class);
        PriceBroadcaster broadcaster = mock(PriceBroadcaster.class);
        when(registry.activeSymbols()).thenReturn(Set.of());
        ScheduledPriceFetchLoop loop = new ScheduledPriceFetchLoop(registry, query, broadcaster);

        // when
        loop.tick();

        // then
        verifyNoInteractions(query);
        verifyNoInteractions(broadcaster);
    }

    @Test
    void tick_broadcastsPrice_whenSymbolFetchSucceeds() {
        // given
        SubscriptionRegistry registry = mock(SubscriptionRegistry.class);
        StockPriceQuery query = mock(StockPriceQuery.class);
        PriceBroadcaster broadcaster = mock(PriceBroadcaster.class);
        StockPrice price = new StockPrice("005930", 71500L, Instant.ofEpochMilli(1L));
        when(registry.activeSymbols()).thenReturn(Set.of("005930"));
        when(query.currentPrice("005930")).thenReturn(price);
        ScheduledPriceFetchLoop loop = new ScheduledPriceFetchLoop(registry, query, broadcaster);

        // when
        loop.tick();

        // then
        verify(broadcaster).broadcastPrice(price);
        verify(broadcaster, never()).sendTo(anyString(), any());
    }

    @Test
    void tick_sendsErrorEventToSubscribers_whenPriceUnavailable() {
        // given
        SubscriptionRegistry registry = mock(SubscriptionRegistry.class);
        StockPriceQuery query = mock(StockPriceQuery.class);
        PriceBroadcaster broadcaster = mock(PriceBroadcaster.class);
        when(registry.activeSymbols()).thenReturn(Set.of("005930"));
        when(registry.sessionsOf("005930")).thenReturn(Set.of("session-1", "session-2"));
        when(query.currentPrice("005930"))
                .thenThrow(new StockPriceUnavailableException("upstream down"));
        ScheduledPriceFetchLoop loop = new ScheduledPriceFetchLoop(registry, query, broadcaster);

        // when
        loop.tick();

        // then
        ArgumentCaptor<ServerEvent> captor = ArgumentCaptor.forClass(ServerEvent.class);
        verify(broadcaster).sendTo(eq("session-1"), captor.capture());
        verify(broadcaster).sendTo(eq("session-2"), captor.capture());
        for (ServerEvent event : captor.getAllValues()) {
            assertThat(event).isInstanceOf(ServerEvent.ErrorEvent.class);
            ServerEvent.ErrorEvent err = (ServerEvent.ErrorEvent) event;
            assertThat(err.code()).isEqualTo(ServerEvent.PRICE_UNAVAILABLE);
            assertThat(err.symbol()).isEqualTo("005930");
        }
        verify(broadcaster, never()).broadcastPrice(any());
    }
}
