package com.example.interviewQuestions.LLD.Code.SplitWise;

import com.example.interviewQuestions.LLD.Code.SplitWise.Enums.SplitType;
import com.example.interviewQuestions.LLD.Code.SplitWise.Ledger.BalanceSheet;
import com.example.interviewQuestions.LLD.Code.SplitWise.Strategy.CalculateSplit;
import com.example.interviewQuestions.LLD.Code.SplitWise.Strategy.EqualSplit;
import com.example.interviewQuestions.LLD.Code.SplitWise.Strategy.ExactSplit;
import com.example.interviewQuestions.LLD.Code.SplitWise.Strategy.PercentageSplit;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class MainClient {
    public static void main(String[] args) {
        User vijay = new User(1, "Vijay");
        User ravi = new User(2, "Ravi");
        User akshat = new User(3, "Akshat");

        Group tripGroup = new Group(1, "Trip");
        tripGroup.addUser(vijay);
        tripGroup.addUser(ravi);
        tripGroup.addUser(akshat);

        Map<SplitType, CalculateSplit> strategies = new HashMap<>();
        strategies.put(SplitType.EQUAL, new EqualSplit());
        strategies.put(SplitType.EXACT, new ExactSplit());
        strategies.put(SplitType.PERCENTAGE, new PercentageSplit());

        SplitWiseManager manager = new SplitWiseManager(new BalanceSheet(), strategies);

        // Vijay pays 900, split equally among the 3
        Expense dinner = new Expense(1, BigDecimal.valueOf(900), SplitType.EQUAL, vijay);
        manager.addExpense(tripGroup, dinner, null);

        // Later: edited to 1200 total, still equal split
        manager.editExpense(tripGroup, dinner, BigDecimal.valueOf(1200), SplitType.EQUAL, null);
    }
}