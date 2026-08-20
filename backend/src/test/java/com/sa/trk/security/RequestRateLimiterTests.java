package com.sa.trk.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RequestRateLimiterTests {

    private final RequestRateLimiter rateLimiter = new RequestRateLimiter(
            Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void blocksRequestsAfterConfiguredLimit() {
        assertThat(rateLimiter.tryAcquire("login", "127.0.0.1", 2, Duration.ofMinutes(1)).allowed())
                .isTrue();
        assertThat(rateLimiter.tryAcquire("login", "127.0.0.1", 2, Duration.ofMinutes(1)).allowed())
                .isTrue();

        RequestRateLimiter.RateLimitDecision blocked = rateLimiter.tryAcquire(
                "login",
                "127.0.0.1",
                2,
                Duration.ofMinutes(1)
        );

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.remaining()).isZero();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void keepsBucketsAndClientsIndependent() {
        rateLimiter.tryAcquire("login", "127.0.0.1", 1, Duration.ofMinutes(1));

        assertThat(rateLimiter.tryAcquire("login", "127.0.0.2", 1, Duration.ofMinutes(1)).allowed())
                .isTrue();
        assertThat(rateLimiter.tryAcquire("ai-analysis", "127.0.0.1", 1, Duration.ofMinutes(1)).allowed())
                .isTrue();
    }
}
