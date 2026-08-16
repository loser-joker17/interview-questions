package com.example.interviewQuestions.LLD.Code.RateLimiter;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Config.RateLimiterConfig;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Enum.AlgorithmType;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Enum.UserType;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.RateLimiter;

public class RateLimiterClient {
    public static void main(String[] args) {

        // Step 1: define config per tier (Free = small limit, Premium = larger limit)
        RateLimiterConfig freeConfig = new RateLimiterConfig(5, 1, 0, 0L);       // 5 tokens capacity, 1 token/sec refill
        RateLimiterConfig premiumConfig = new RateLimiterConfig(100, 10, 0, 0L); // 100 tokens capacity, 10 tokens/sec refill

        // Step 2: use the Factory to build the actual algorithm objects — ONCE, at setup time
        RateLimiter freeLimiter = RateLimiterFactory.createRateLimiter(AlgorithmType.TOKEN_BUCKET, freeConfig);
        RateLimiter premiumLimiter = RateLimiterFactory.createRateLimiter(AlgorithmType.TOKEN_BUCKET, premiumConfig);

        // Step 3: build the Manager and register the pre-built limiters + users
        RateLimiterManager manager = new RateLimiterManager();
        manager.registerLimiter(UserType.FREE, freeLimiter);
        manager.registerLimiter(UserType.PREMIUM, premiumLimiter);

        manager.registerUser("user_free_1", UserType.FREE);
        manager.registerUser("user_premium_1", UserType.PREMIUM);

        // Step 4: simulate incoming requests
        Request req1 = new Request("user_free_1", "127.0.0.1");

        for (int i = 1; i <= 7; i++) {
            boolean allowed = manager.allowRequest(req1);
            System.out.println("Free user request #" + i + " -> " + (allowed ? "ALLOWED" : "REJECTED (429)"));
        }

        Request req2 = new Request("user_premium_1", "127.0.0.2");
        boolean premiumAllowed = manager.allowRequest(req2);
        System.out.println("Premium user request -> " + (premiumAllowed ? "ALLOWED" : "REJECTED (429)"));

        // Step 5: unregistered user — should be rejected (fail closed)
        Request req3 = new Request("unknown_user", "127.0.0.3");
        boolean unknownAllowed = manager.allowRequest(req3);
        System.out.println("Unregistered user request -> " + (unknownAllowed ? "ALLOWED" : "REJECTED (429)"));
    }
}