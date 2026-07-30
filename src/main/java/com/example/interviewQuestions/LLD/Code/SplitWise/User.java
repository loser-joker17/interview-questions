package com.example.interviewQuestions.LLD.Code.SplitWise;

public class User {
    private final int userId;
    private String userName;

    public User(int userId,String userName){
        this.userId=userId;
        this.userName=userName;
    }
    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
