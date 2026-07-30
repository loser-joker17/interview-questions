package com.example.interviewQuestions.LLD.Code.SplitWise;

import com.example.interviewQuestions.LLD.Code.SplitWise.Ledger.BalanceSheet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitWiseManager {
    private Map<Group, List<Expense>> shareExpenses;
    private BalanceSheet balanceSheet;

    public SplitWiseManager(BalanceSheet balanceSheet){
        this.balanceSheet=balanceSheet;
        this.shareExpenses=new HashMap<>();
    }
    public void addExpense(Group group,Expense expense){
        shareExpenses.computeIfAbsent(group,k-> new ArrayList<>()).add(expense);

        User creditor = expense.getExpensePaidBy();

        for(Map.Entry<User, BigDecimal> entry : expense.getExpenses().entrySet()){
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
