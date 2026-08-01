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
    private Map<Group, List<Expense>> shareExpenses;
    private BalanceSheet balanceSheet;
    Private final CalculateSplit calculateSplit;

    public SplitWiseManager(BalanceSheet balanceSheet,CalculateSplit calculateSplit){
        this.balanceSheet=balanceSheet;
        this.shareExpenses=new HashMap<>();
        this.calculateSplit=calculateSplit;
    }
    public void addExpense(Group group,Expense expense,Map<User,BigDecimal>inputValues){
        shareExpenses.computeIfAbsent(group,k-> new ArrayList<>()).add(expense);

        User creditor = expense.getExpensePaidBy();

        Map<User,BigDecimal> shares = new HashMap<>();
        if(expense.getSplitType()==SplitType.EQUAL){
            shares = calculateSplit.expenseCalculation(expense,group,inputValues);
        }

        for(Map.Entry<User, BigDecimal> entry : shares.entrySet()){
            User debitor = entry.getKey();
            BigDecimal amount = entry.getValue();

            if(!debitor.equals(creditor)){
                balanceSheet.updateAmount(creditor,debitor,amount);
            }
        }
    }
    public void editExpense(Group group,Expense expense){
        List<Expense> expenses = shareExpenses.get(group);

        balanceSheet.updateBalance()

    }
    public void deleteExpense(Group group,Expense expense){
        List<Expense> expenses = shareExpenses.get(group);

        if(expenses==null){
            return;
        }
        expenses.remove(expense);
    }
}
