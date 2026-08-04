package com.example.interviewQuestions.LLD.Code.SplitWise.Strategy;

import com.example.interviewQuestions.LLD.Code.SplitWise.Expense;
import com.example.interviewQuestions.LLD.Code.SplitWise.Group;
import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ExactSplit implements CalculateSplit {
    @Override
    public Map<User, BigDecimal> expenseCalculation(Expense expense, Group group, Map<User, BigDecimal> inputValues) {
        BigDecimal amount = expense.getAmount();
        System.out.println("Exact Split Called");

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : inputValues.values()) sum = sum.add(v);

        if (sum.compareTo(amount) != 0) {
            throw new IllegalArgumentException("Exact amounts must sum to total expense amount");
        }
        return new HashMap<>(inputValues);
    }
}