package com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucket implements RateLimiter {
    private int capacity;
    private int riffleRate;
    Map<String,Integer> tokens;
    Map<String,Long> riffleRateStamp;

    public TokenBucket(int capacity,int riffleRate){
        this.capacity=capacity;
        this.riffleRate=riffleRate;
        this.tokens = new ConcurrentHashMap<>();
        this.riffleRateStamp = new ConcurrentHashMap<>();
    }
    @Override
    public boolean allowRate(Request request){

    }
}
