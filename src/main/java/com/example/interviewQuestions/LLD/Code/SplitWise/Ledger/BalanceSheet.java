package com.example.interviewQuestions.LLD.Code.SplitWise.Ledger;

import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BalanceSheet {
    Map<User,Map<User, BigDecimal>> balances;

    public BalanceSheet(){
        this.balances = new HashMap<>();
    }

    public void updateBalance(User creditor,User debtor, BigDecimal amount){

    }

    public void reverseBalance(User debtor, User creditor, BigDecimal amount){

    }
    public void removeBalance(User creditor,User debtor){
        Map<User, BigDecimal> map = balances.get(debtor);
        if (map == null)
            return;

        map.remove(creditor);
        if (map.isEmpty()) {
            balances.remove(debtor);
        }
    }
    public BigDecimal getBalance(User debtor, User creditor) {
        return balances.getOrDefault(debtor, Collections.emptyMap())
                .getOrDefault(creditor, BigDecimal.ZERO);
    }


    public Map<User,BigDecimal> getAllBalance(User debtor){
        return balances.getOrDefault(debtor, Collections.emptyMap());
    }

}
