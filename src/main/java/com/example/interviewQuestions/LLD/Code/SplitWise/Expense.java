package com.example.interviewQuestions.LLD.Code.SplitWise;

import com.example.interviewQuestions.LLD.Code.SplitWise.Enums.SplitType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Expense {
    private final int expenseId;
    private final User expensePaidBy;
    private BigDecimal amount;
    private final Map<User, BigDecimal> expenses;
    private SplitType splitType;

    public Expense(int expenseId,BigDecimal amount,SplitType splitType,User expensePaidBy) {
        this.amount = amount;
        this.expenseId = expenseId;
        this.splitType = splitType;
        this.expensePaidBy = expensePaidBy;
        this.expenses = new HashMap<>();
    }

    public int getExpenseId() {
        return expenseId;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Map<User, BigDecimal> getExpenses() {
        return expenses;
    }

    public User getExpensePaidBy() {
        return expensePaidBy;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }
}
