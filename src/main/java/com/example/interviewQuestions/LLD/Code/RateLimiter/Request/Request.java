package com.example.interviewQuestions.LLD.Code.RateLimiter.Request;

public class Request {
    private final String userId;
    private final String Ip;

    public Request(String userId,String Ip){
        this.Ip=Ip;
        this.userId=userId;
    }

    public String getIp() {
        return Ip;
    }

    public String getUserId() {
        return userId;
    }
}
