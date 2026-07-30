package com.example.interviewQuestions.LLD.Code.SplitWise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SplitWiseManager {
    private Group group;
    private Expense expense;
    private Map<Group, List<Expense>> manageExpenses;

    public void addExpense(Group group,Expense expense){

        manageExpenses.put(group,k-> new ArrayList<>(expense));
    }
    public void editExpense(Group group,Expense expense){
        int groupId = group.getGroupId();
        int expenseId = expense.getExpenseId();

    }
    public void deleteExpense(Group group){
        int groupId= group.getGroupId();

        if(manageExpenses.containsKey(group)){
            manageExpenses.remove(group);
        }
    }
}
