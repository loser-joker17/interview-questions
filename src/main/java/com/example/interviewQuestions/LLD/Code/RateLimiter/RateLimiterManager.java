package com.example.interviewQuestions.LLD.Code.RateLimiter;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Enum.UserType;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.RateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterManager {
    private final Map<UserType, RateLimiter> tierToLimiter;
    private final Map<String, UserType> userToTier;   // fixed key type too — String userId, not Request

    public RateLimiterManager() {
        this.tierToLimiter = new ConcurrentHashMap<>();
        this.userToTier = new ConcurrentHashMap<>();
    }

    public void registerLimiter(UserType tier, RateLimiter limiter) {
        tierToLimiter.put(tier, limiter);
    }

    public void registerUser(String userId, UserType tier) {
        userToTier.put(userId, tier);
    }

    public boolean allowRequest(Request request) {
        String userId = request.getUserId();
        UserType userType = userToTier.get(userId);
        if(userType == null){
            return false;
        }
        RateLimiter limiter = tierToLimiter.get(userType);
        boolean result = limiter.allowRequest(request);
        return result;
    }
}
