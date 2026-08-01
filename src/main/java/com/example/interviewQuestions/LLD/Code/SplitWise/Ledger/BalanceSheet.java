package com.example.interviewQuestions.LLD.Code.SplitWise.Ledger;

import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BalanceSheet {
    private final Map<User, Map<User, BigDecimal>> balances;

    public BalanceSheet() {
        this.balances = new HashMap<>();
    }

    /**
     * Records that `debtor` owes `creditor` an additional `amount`.
     * Single-direction, netted-on-write ledger:
     *   balances[debtor][creditor] = amount owed
     * If a reverse debt already exists, it's netted against this update
     * rather than stored as a separate opposing entry.
     */
    public void updateBalance(User creditor, User debtor, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal existingForward = getBalance(debtor, creditor);
        BigDecimal existingReverse = getBalance(creditor, debtor);

        if (existingReverse.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal net = existingReverse.subtract(amount);
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                setBalance(creditor, debtor, net); // reverse debt shrinks
            } else if (net.compareTo(BigDecimal.ZERO) < 0) {
                removeBalance(creditor, debtor);
                setBalance(debtor, creditor, net.negate()); // direction flips
            } else {
                removeBalance(creditor, debtor); // exactly cancels — no dangling zero
            }
        } else {
            setBalance(debtor, creditor, existingForward.add(amount));
        }
    }

    private void setBalance(User debtor, User creditor, BigDecimal amount) {
        balances.computeIfAbsent(debtor, k -> new HashMap<>()).put(creditor, amount);
    }

    public void removeBalance(User creditor, User debtor) {
        Map<User, BigDecimal> map = balances.get(debtor);
        if (map == null) return;
        map.remove(creditor);
        if (map.isEmpty()) balances.remove(debtor);
    }

    public BigDecimal getBalance(User debtor, User creditor) {
        return balances.getOrDefault(debtor, Collections.emptyMap())
                .getOrDefault(creditor, BigDecimal.ZERO);
    }

    public Map<User, BigDecimal> getAllBalance(User debtor) {
        return balances.getOrDefault(debtor, Collections.emptyMap());
    }
}