package com.example.interviewQuestions.LLD.Code.SplitWise;

import com.example.interviewQuestions.LLD.Code.SplitWise.Enums.SplitType;
import com.example.interviewQuestions.LLD.Code.SplitWise.Ledger.BalanceSheet;
import com.example.interviewQuestions.LLD.Code.SplitWise.Strategy.CalculateSplit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitWiseManager {
    private final Map<Group, List<Expense>> shareExpenses;
    private final BalanceSheet balanceSheet;
    private final Map<SplitType, CalculateSplit> strategies; // registry — OCP-compliant

    public SplitWiseManager(BalanceSheet balanceSheet, Map<SplitType, CalculateSplit> strategies) {
        this.balanceSheet = balanceSheet;
        this.shareExpenses = new HashMap<>();
        this.strategies = strategies;
    }

    public void addExpense(Group group, Expense expense, Map<User, BigDecimal> inputValues) {
        shareExpenses.computeIfAbsent(group, k -> new ArrayList<>()).add(expense);

        CalculateSplit strategy = strategies.get(expense.getSplitType());
        Map<User, BigDecimal> shares = strategy.expenseCalculation(expense, group, inputValues);
        expense.getShares().putAll(shares);

        applyShares(expense.getExpensePaidBy(), shares, false);
    }

    public void editExpense(Group group, Expense oldExpense, BigDecimal newAmount,
                            SplitType newSplitType, Map<User, BigDecimal> newInputValues) {
        // 1. Reverse the old expense's effect on the ledger
        applyShares(oldExpense.getExpensePaidBy(), oldExpense.getShares(), true);

        // 2. Update the expense's own fields
        oldExpense.setAmount(newAmount);
        oldExpense.setSplitType(newSplitType);

        // 3. Recompute and apply the new split
        CalculateSplit strategy = strategies.get(newSplitType);
        Map<User, BigDecimal> newShares = strategy.expenseCalculation(oldExpense, group, newInputValues);
        oldExpense.getShares().clear();
        oldExpense.getShares().putAll(newShares);

        applyShares(oldExpense.getExpensePaidBy(), newShares, false);
    }

    public void deleteExpense(Group group, Expense expense) {
        List<Expense> expenses = shareExpenses.get(group);
        if (expenses == null) return;

        applyShares(expense.getExpensePaidBy(), expense.getShares(), true);
        expenses.remove(expense);
    }

    private void applyShares(User payer, Map<User, BigDecimal> shares, boolean reverse) {
        for (Map.Entry<User, BigDecimal> entry : shares.entrySet()) {
            User debtor = entry.getKey();
            BigDecimal amount = entry.getValue();
            if (!debtor.equals(payer)) {
                balanceSheet.updateBalance(payer, debtor, reverse ? amount.negate() : amount);
            }
        }
    }
}