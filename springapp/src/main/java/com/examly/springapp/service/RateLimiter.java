package com.examly.springapp.service;

/**
 * Interface defining rate limiting contracts.
 * Permits in-memory sliding window implementations as well as
 * distributed Redis-backed or API Gateway enforcement for multi-replica deployments.
 */
public interface RateLimiter {

    /**
     * Checks if a request for the given key is allowed under default rate limits.
     *
     * @param key unique bucket identifier (e.g., "ip:192.0.2.1" or "action:user")
     * @return true if request is within limits, false if throttled
     */
    boolean isAllowed(String key);

    /**
     * Checks if a request for the given key is allowed under a custom request quota within the window.
     *
     * @param key unique bucket identifier
     * @param maxRequests maximum permitted requests within the rate window
     * @return true if request is within limits, false if throttled
     */
    boolean isAllowed(String key, int maxRequests);

    /**
     * Returns the current number of tracked rate limit keys.
     */
    int size();

    /**
     * Resets/clears all rate limit buckets.
     */
    void clear();
}
