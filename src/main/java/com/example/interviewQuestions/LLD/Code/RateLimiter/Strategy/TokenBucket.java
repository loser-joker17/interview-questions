package com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucket implements RateLimiter {
    private int capacity;
    private int riffleRate;
    Map<String,Integer> tokens;
    Map<String,Long> lastReffilTimeStamp;

    public TokenBucket(int capacity,int riffleRate){
        this.capacity=capacity;
        this.riffleRate=riffleRate;
        this.tokens = new ConcurrentHashMap<>();
        this.lastReffilTimeStamp = new ConcurrentHashMap<>();
    }
    @Override
    public synchronized boolean allowRequest(Request request){
        String userId = request.getUserId();
        Long currentTimeStamp = System.currentTimeMillis();  // as the token are puting at rate
        lastReffilTimeStamp.putIfAbsent(userId, currentTimeStamp);
        tokens.putIfAbsent(userId,capacity);
        Long lastRefill = lastReffilTimeStamp.get(userId);
        Long elapsedMillis = currentTimeStamp - lastReffilTimeStamp.get(userId);
        Long elapsedSeconds = elapsedMillis / 1000;
        int tokensToAdd = (int) (elapsedSeconds * riffleRate);
        if(elapsedSeconds>0){
            int currentTokens = tokens.get(userId);
            int newTokenCount = Math.min(capacity, currentTokens + tokensToAdd);
            tokens.put(userId,newTokenCount);
            lastReffilTimeStamp.putIfAbsent(userId,currentTimeStamp);
        }
        if(tokens.get(userId)>0){
            tokens.put(userId,tokens.get(userId)-1);
            return true;
        }
        return false;
    }
}
