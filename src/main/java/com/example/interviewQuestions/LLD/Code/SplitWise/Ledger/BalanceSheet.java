package com.example.interviewQuestions.LLD.Code.SplitWise.Ledger;

import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class BalanceSheet {
    Map<User,Map<User, BigDecimal>> balances;

    public BalanceSheet(){
        this.balances = new HashMap<>();
    }

    public void updateBalance(User creditor,User debtor, BigDecimal amount){

    }

    public void removeBalance(User creditor,User debtor){

    }
    public BigDecimal getBalance(User creditor,User debtor){

    }

    public Map<User,BigDecimal> getAllBalance(User creditor,User debtor){

    }

}
