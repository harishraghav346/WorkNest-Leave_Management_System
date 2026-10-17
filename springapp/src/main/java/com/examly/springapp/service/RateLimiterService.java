package com.examly.springapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory sliding-window request rate limiter.
 *
 * NOTE: This rate limiter is strictly in-memory and operates per application instance.
 * It is not a distributed rate limiter. In clustered or multi-instance deployments,
 * a distributed rate limiter (such as Redis or an API Gateway token bucket) must be used.
 */
@Service
public class RateLimiterService implements RateLimiter {

    private static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 15;
    private static final long WINDOW_SECONDS = 60;
    private static final int DEFAULT_MAX_CAPACITY = 5000;

    private final int maxCapacity;

    private static class RequestBucket {
        long windowStart;
        int count;

        RequestBucket(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    // Thread-safe bounded LRU map with maximum capacity protection
    private final Map<String, RequestBucket> buckets;

    public RateLimiterService() {
        this(DEFAULT_MAX_CAPACITY);
    }

    public RateLimiterService(@Value("${app.ratelimit.max-capacity:5000}") int maxCapacity) {
        this.maxCapacity = Math.max(10, maxCapacity);
        this.buckets = new LinkedHashMap<String, RequestBucket>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, RequestBucket> eldest) {
                return size() > RateLimiterService.this.maxCapacity;
            }
        };
    }

    /**
     * Checks if a request for the given key is allowed under default rate limits (15 req/min).
     */
    public synchronized boolean isAllowed(String key) {
        return isAllowed(key, DEFAULT_MAX_REQUESTS_PER_MINUTE);
    }

    /**
     * Checks if a request for the given key is allowed under custom maxRequests within the 60-second window.
     */
    public synchronized boolean isAllowed(String key, int maxRequests) {
        long now = Instant.now().getEpochSecond();
        pruneExpiredEntries(now);

        RequestBucket bucket = buckets.get(key);
        if (bucket == null || (now - bucket.windowStart) >= WINDOW_SECONDS) {
            buckets.put(key, new RequestBucket(now, 1));
            return true;
        } else {
            bucket.count++;
            return bucket.count <= maxRequests;
        }
    }

    /**
     * Prunes expired buckets when the map size grows past half capacity.
     */
    private void pruneExpiredEntries(long now) {
        if (buckets.size() > (maxCapacity / 2)) {
            Iterator<Map.Entry<String, RequestBucket>> it = buckets.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, RequestBucket> entry = it.next();
                if ((now - entry.getValue().windowStart) >= WINDOW_SECONDS) {
                    it.remove();
                } else {
                    break;
                }
            }
        }
    }

    public synchronized int size() {
        return buckets.size();
    }

    public synchronized void clear() {
        buckets.clear();
    }
}
