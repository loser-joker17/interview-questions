package com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;

public interface RateLimiter {
    public boolean allowRequest(Request request);
}
