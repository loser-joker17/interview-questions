package com.example.interviewQuestions.LLD.Code.SplitWise;

import com.example.interviewQuestions.LLD.Code.SplitWise.Enums.SplitType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Expense {
    private final int expenseId;
    private final User expensePaidBy;
    private BigDecimal amount;
    private SplitType splitType;
    // Always holds FINAL, absolute-currency shares — never raw percentages
    // or unvalidated raw input. Each CalculateSplit implementation
    // normalizes its own input format into this before returning.
    private final Map<User, BigDecimal> shares;

    public Expense(int expenseId, BigDecimal amount, SplitType splitType, User expensePaidBy) {
        this.expenseId = expenseId;
        this.amount = amount;
        this.splitType = splitType;
        this.expensePaidBy = expensePaidBy;
        this.shares = new HashMap<>();
    }

    public int getExpenseId() { return expenseId; }
    public SplitType getSplitType() { return splitType; }
    public BigDecimal getAmount() { return amount; }
    public Map<User, BigDecimal> getShares() { return shares; }
    public User getExpensePaidBy() { return expensePaidBy; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
}