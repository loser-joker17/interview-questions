package com.example.interviewQuestions.LLD.Code.SplitWise.Strategy;

import com.example.interviewQuestions.LLD.Code.SplitWise.Expense;
import com.example.interviewQuestions.LLD.Code.SplitWise.Group;
import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExactSplit implements CalculateSplit{

    @Override
    public Map<User, BigDecimal> expenseCalculation(Expense expense, Group group,Map<User,BigDecimal>inputValues){
        BigDecimal amount = expense.getAmount();
        List<User> userList = group.getUserList();

        BigDecimal shareAmount = amount;

        Map<User,BigDecimal> result = new HashMap<>();

        for(User user : userList){
            result.put(user,shareAmount);
        }
        return result;
    }
}
