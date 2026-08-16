package com.example.interviewQuestions.LLD.Code.RateLimiter.Config;

public class RateLimiterConfig {
    private final int capacity;        // used by TokenBucket, LeakingBucket
    private final int refillRate;      // used by TokenBucket
    private final int maxRequests;     // used by FixedWindow, SlidingWindow variants
    private final Long windowSizeSeconds; // used by FixedWindow, SlidingWindow variants

    public RateLimiterConfig(int capacity, int refillRate, int maxRequests, Long windowSizeSeconds) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.maxRequests = maxRequests;
        this.windowSizeSeconds = windowSizeSeconds;
    }

    public int getCapacity() { return capacity; }
    public int getRefillRate() { return refillRate; }
    public int getMaxRequests() { return maxRequests; }
    public Long getWindowSizeSeconds() { return windowSizeSeconds; }
}
