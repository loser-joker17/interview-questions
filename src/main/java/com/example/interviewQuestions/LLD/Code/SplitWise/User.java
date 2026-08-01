package com.example.interviewQuestions.LLD.Code.SplitWise;

public class User {
    private final int userId;
    private String userName;

    public User(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    // NOTE: override equals()/hashCode() based on userId — User is used as
    // a map key throughout BalanceSheet and Expense.shares. Without this,
    // two objects representing the same person are treated as different
    // keys by HashMap's default identity-based equality.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return userId == ((User) o).userId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(userId);
    }
}