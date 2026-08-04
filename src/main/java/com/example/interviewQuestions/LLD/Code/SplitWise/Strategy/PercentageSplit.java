package com.example.interviewQuestions.LLD.Code.SplitWise.Strategy;

import com.example.interviewQuestions.LLD.Code.SplitWise.Expense;
import com.example.interviewQuestions.LLD.Code.SplitWise.Group;
import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class PercentageSplit implements CalculateSplit {
    @Override
    public Map<User, BigDecimal> expenseCalculation(Expense expense, Group group, Map<User, BigDecimal> inputValues) {
        System.out.println("Percentage Split called");
        BigDecimal amount = expense.getAmount();
        BigDecimal totalPercentage = BigDecimal.ZERO;
        Map<User, BigDecimal> result = new HashMap<>();

        for (Map.Entry<User, BigDecimal> entry : inputValues.entrySet()) {
            User user = entry.getKey();
            BigDecimal percentage = entry.getValue();
            totalPercentage = totalPercentage.add(percentage);

            BigDecimal shareAmount = amount.multiply(percentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            result.put(user, shareAmount);
        }

        if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("Percentages must add up to 100");
        }
        return result;
    }
}