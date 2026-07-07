package br.com.finlumia.identify.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.finlumia.shared.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000L;

    private record Window(long startMs, AtomicInteger count) {}

    private final ConcurrentHashMap<String, Window> ipWindows = new ConcurrentHashMap<>();

    /**
     * Checks and consumes one attempt slot for the given IP.
     * Throws RateLimitException with retryAfterSeconds if the limit is exceeded.
     */
    public void consumeForIp(String ip) {
        long now = System.currentTimeMillis();
        long windowStart = (now / WINDOW_MS) * WINDOW_MS;

        Window window = ipWindows.compute(ip, (key, existing) -> {
            if (existing == null || existing.startMs() < windowStart) {
                return new Window(windowStart, new AtomicInteger(0));
            }
            return existing;
        });

        int attempts = window.count().incrementAndGet();
        if (attempts > MAX_ATTEMPTS) {
            long retryAfterMs = window.startMs() + WINDOW_MS - now;
            long retryAfterSeconds = Math.max(1L, retryAfterMs / 1000L + 1L);
            log.warn("RATE_LIMIT_EXCEEDED ip={} attempts={}", ip, attempts);
            throw new RateLimitException(retryAfterSeconds);
        }
    }

    @Scheduled(fixedDelay = 300_000L)
    public void evictStaleWindows() {
        long threshold = System.currentTimeMillis() - WINDOW_MS * 2;
        ipWindows.entrySet().removeIf(e -> e.getValue().startMs() < threshold);
    }
}
