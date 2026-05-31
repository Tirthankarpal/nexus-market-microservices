package com.nexus.apigateway.rate;

/**
 * Thread-safe implementation of the Token Bucket algorithm.
 */
public class TokenBucket {
    private final long capacity;
    private final long refillTokens;
    private final long refillPeriodMs;
    
    private double tokens;
    private long lastRefillTime;

    public TokenBucket(long capacity, long refillTokens, long refillPeriodMs) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriodMs = refillPeriodMs;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    /**
     * Attempts to consume a single token.
     * @return true if token is consumed successfully, false if the bucket is empty.
     */
    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;
        if (elapsed > 0) {
            double tokensToAdd = (double) elapsed * refillTokens / refillPeriodMs;
            tokens = Math.min(capacity, tokens + tokensToAdd);
            lastRefillTime = now;
        }
    }
}
