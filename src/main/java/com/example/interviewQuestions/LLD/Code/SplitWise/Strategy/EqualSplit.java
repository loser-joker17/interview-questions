package com.example.interviewQuestions.LLD.Code.SplitWise.Strategy;

import com.example.interviewQuestions.LLD.Code.SplitWise.Expense;
import com.example.interviewQuestions.LLD.Code.SplitWise.Group;
import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EqualSplit implements CalculateSplit {
    @Override
    public Map<User, BigDecimal> expenseCalculation(Expense expense, Group group, Map<User, BigDecimal> inputValues) {
        BigDecimal amount = expense.getAmount();
        List<User> userList = group.getUserList();
        int n = userList.size();

        BigDecimal shareAmount = amount.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal totalAssigned = shareAmount.multiply(BigDecimal.valueOf(n));
        BigDecimal remainder = amount.subtract(totalAssigned); // leftover cents

        Map<User, BigDecimal> result = new HashMap<>();
        for (int i = 0; i < n; i++) {
            User user = userList.get(i);
            BigDecimal share = shareAmount;
            if (i == 0) share = share.add(remainder); // first user absorbs the leftover
            result.put(user, share);
        }
        return result;
    }
}