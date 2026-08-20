package com.sa.trk.security;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class RequestRateLimiter {

    private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(5).toMillis();

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupAt = new AtomicLong();
    private final Clock clock;

    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public RateLimitDecision tryAcquire(
            String bucket,
            String clientKey,
            int configuredLimit,
            Duration configuredWindow) {
        int limit = Math.max(1, configuredLimit);
        long windowMillis = Math.max(1_000L, safeWindow(configuredWindow).toMillis());
        long now = clock.millis();
        cleanupExpiredCounters(now);

        String counterKey = normalizeKey(bucket) + ':' + normalizeKey(clientKey);
        DecisionHolder holder = new DecisionHolder();
        counters.compute(counterKey, (ignored, current) -> {
            if (current == null || current.expiresAtMillis <= now) {
                holder.decision = new RateLimitDecision(true, limit - 1, 0);
                return new WindowCounter(1, now + windowMillis);
            }
            if (current.count >= limit) {
                long retryAfterSeconds = Math.max(1L, (current.expiresAtMillis - now + 999L) / 1_000L);
                holder.decision = new RateLimitDecision(false, 0, retryAfterSeconds);
                return current;
            }
            current.count++;
            holder.decision = new RateLimitDecision(true, limit - current.count, 0);
            return current;
        });
        return holder.decision;
    }

    private Duration safeWindow(Duration configuredWindow) {
        return configuredWindow == null || configuredWindow.isNegative() || configuredWindow.isZero()
                ? Duration.ofMinutes(1)
                : configuredWindow;
    }

    private String normalizeKey(String value) {
        String normalized = value == null || value.isBlank() ? "unknown" : value.trim();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200);
    }

    private void cleanupExpiredCounters(long now) {
        long previousCleanup = lastCleanupAt.get();
        if (now - previousCleanup < CLEANUP_INTERVAL_MILLIS
                || !lastCleanupAt.compareAndSet(previousCleanup, now)) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis <= now);
    }

    private static final class WindowCounter {
        private int count;
        private final long expiresAtMillis;

        private WindowCounter(int count, long expiresAtMillis) {
            this.count = count;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private static final class DecisionHolder {
        private RateLimitDecision decision;
    }

    public record RateLimitDecision(boolean allowed, int remaining, long retryAfterSeconds) {
    }
}
