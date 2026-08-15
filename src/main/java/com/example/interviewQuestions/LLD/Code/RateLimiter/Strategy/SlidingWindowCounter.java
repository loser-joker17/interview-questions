package com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;

public class SlidingWindowCounter implements RateLimiter{
    @Override
    public synchronized boolean allowRate(Request request){
        return true;
    }
}
