package com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowFixed implements RateLimiter{
    private int maxRequest;
    private Long windowStart;
    private Long windowSizeMills;
    private Map<String,Integer> requestCounts;

    public SlidingWindowFixed(int maxRequest,Long windowSizeMills){
        this.maxRequest=maxRequest;
        this.windowSizeMills=windowSizeMills;
        this.windowStart=System.currentTimeMillis();
        this.requestCounts=new ConcurrentHashMap<>();
    }
    @Override
    public synchronized boolean allowRequest(Request request){
        Long currentTime = System.currentTimeMillis();
        String userId = request.getUserId();
        if(currentTime-windowStart>=windowSizeMills){
            requestCounts.remove(userId);
            windowStart=currentTime;
        }
        requestCounts.put(userId,requestCounts.getOrDefault(userId,0)+1);
        if(requestCounts.get(userId)>=maxRequest){
            return false;
        }
        return true;
    }
}
