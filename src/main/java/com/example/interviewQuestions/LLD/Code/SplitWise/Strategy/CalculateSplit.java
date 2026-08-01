package com.example.interviewQuestions.LLD.Code.SplitWise.Strategy;

import com.example.interviewQuestions.LLD.Code.SplitWise.Expense;
import com.example.interviewQuestions.LLD.Code.SplitWise.Group;
import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.util.Map;

public interface CalculateSplit {
    Map<User, BigDecimal> expenseCalculation(Expense expense, Group group,Map<User,BigDecimal>inputValues);
}
