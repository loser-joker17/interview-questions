package com.example.interviewQuestions.LLD.Code.RateLimiter;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Config.RateLimiterConfig;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Enum.AlgorithmType;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.RateLimiter;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.SlidingWindowFixed;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.TokenBucket;

public class RateLimiterFactory {
    public static RateLimiter createRateLimiter(AlgorithmType algorithmType, RateLimiterConfig rateLimiterConfig){
        switch (algorithmType){
            case FIXED_WINDOW:
                return new SlidingWindowFixed(rateLimiterConfig.getMaxRequests(),rateLimiterConfig.getWindowSizeSeconds());
            case TOKEN_BUCKET:
                return new TokenBucket(rateLimiterConfig.getCapacity(), rateLimiterConfig.getRefillRate());
            default:
                throw new IllegalArgumentException("Unknown algorithm type: " + algorithmType);
        }
    }
}
