# Splitwise-style Expense Sharing System — LLD Interview Notes

A full record of the mock Low-Level Design interview: requirements, design
decisions, doubts raised and resolved, trade-offs, the deep-dive Q&A round,
and final code.

---

## 1. Requirements Gathered

| Question | Decision |
|---|---|
| Support more than 2 people? | Yes — any number of people in a group; an expense can involve any subset of the group, not necessarily everyone. |
| Expense categories (food, travel...)? | Optional, low-priority tag — not core to the design. |
| Multi-currency (INR, USD, JPY)? | Out of scope — single currency only. |
| Notifications, analytics, payment tracking? | Out of scope — focus purely on splitting + balances. |
| Edit / delete expenses? | Yes — both supported; balances must update correctly, not just additively. |
| Anyone in the group can add an expense? | Yes — no special admin role. |
| Which split types? | All three: **EQUAL, EXACT, PERCENTAGE** — with validation (percentages sum to 100, exact amounts sum to total). |
| Rounding? | Must be handled deterministically for EQUAL splits — someone absorbs the leftover cent. |
| Settlement ("settle up")? | Yes — a direct payment between two people that reduces/zeroes a balance; distinct from adding an expense. |
| Simplified balances? | System should be able to compute a simplified "who owes whom" view, minimizing transaction count, on demand. |

---

## 2. Core Entities & Responsibilities

- **`User`** — id, name.
- **`Group`** — id, name, member list.
- **`Expense`** — payer, total amount, split type, and a **`shares` map** (`Map<User, BigDecimal>`) holding each participant's final computed share.
- **`CalculateSplit`** (interface / Strategy pattern) — one implementation per split type: `EqualSplit`, `ExactSplit`, `PercentageSplit`.
- **`SplitWiseManager`** — owns groups and their expenses (`Map<Group, List<Expense>>`); adds/edits/deletes expenses; delegates split math to the right strategy; drives ledger updates.
- **`BalanceSheet`** — the live, incrementally-updated ledger (`Map<User, Map<User, BigDecimal>>`); the single source of truth for "who owes whom."

---

## 3. Doubts Raised & Gaps Filled (Q&A Log)

### Q: Who decides the percentage split for `PercentageSplit`?
**Doubt:** should the system infer/define percentage criteria itself?

**Resolution:** No — the percentage (like the exact amount for `ExactSplit`) is
**caller-supplied input**, not something the system decides. The system's job
is only to **validate** that input (percentages sum to 100, exact amounts sum
to total) and compute resulting `BigDecimal` shares. Only `EqualSplit`
computes independently from just the participant list and total.

### Q: What does `Expense.shares` actually represent — final amounts, percentages, or exact inputs?
**Gap filled (originally left unanswered mid-session):** It always represents
**final calculated shares in absolute currency**, regardless of split type.
Never raw percentages, never unvalidated raw input. Each `CalculateSplit`
implementation is responsible for converting its own input format
(percentage, exact, or nothing at all for equal) into this single normalized
output format before it's stored. This is precisely why `PercentageSplit`
must multiply `percentage × total ÷ 100` before writing to the map — storing
a raw "30" meaning "30%" would corrupt any downstream code (like
`BalanceSheet`) that expects a currency amount.

### Q: Single `CalculateSplit` field vs. a strategy registry?
**Doubt surfaced via bug:** initial `SplitWiseManager` had one
`CalculateSplit calculateSplit` field injected at construction — meaning the
manager was permanently locked to whichever strategy was passed in. Adding an
`EQUAL` expense then a `PERCENTAGE` expense would silently run the second
one through the wrong strategy, producing incorrect shares with **no error
raised**.

**Resolution:** Replace the single field with `Map<SplitType, CalculateSplit>
strategies`, looked up per-call via `strategies.get(expense.getSplitType())`.

**SOLID connection:** the original `if (splitType == EQUAL) {...}` branch in
`addExpense()` violates the **Open/Closed Principle** — every new split type
(e.g., a hypothetical `SHARE_BY_WEIGHT`) would require **modifying existing,
already-tested code**. The registry approach fixes this: adding a new split
type means writing one new class and registering it — `addExpense()` itself
never changes again.

### Q: Why not `TreeSet<Request>`-style objects... (carried over instinct from Round 1) — applied here as: why not store both debt directions?
**Doubt:** should `BalanceSheet` store `balance[A][B]` and `balance[B][A]`
redundantly?

**Resolution — single direction, netted on write:**
`balances[debtor][creditor] = amount owed`. Before adding a new debt, check
whether the **reverse** debt already exists; if so, net the two amounts and
keep only the remaining balance in one direction.

**Why, precisely (not just "avoids duplicates"):**
- **No sync risk** — dual-direction storage means every update must touch 2
  entries in lockstep, or a bug leaves them inconsistent (e.g., `A→B` updated
  but `B→A` not adjusted to match).
- **Simpler reads** — `getBalance(debtor, creditor)` is one lookup, not "read
  both directions and subtract."
- **Enforces a normalized-ledger invariant** — at most one live entry between
  any two users at a time.

### Q: What happens when a net balance hits exactly ₹0?
**Gap filled:** per the "normalized ledger" principle, a zero balance should
be **removed from the map entirely**, not left as a dangling `0` entry — a
normalized ledger with stale zero rows isn't actually normalized, and would
show a false "still owes ₹0" row in any UI built on top of it.

### Q: What happens when netting causes the debt direction to *flip* (not just shrink)?
**Gap filled with a concrete trace:** Bob owes Alice ₹100. A new expense
makes Alice owe Bob ₹150 (larger than the existing reverse debt). The
correct behavior: remove the old `balances[Bob][Alice]` entry, and create a
new `balances[Alice][Bob] = 50` (150 − 100). The netting logic must handle
all three cases from a single `updateBalance()` call: reverse debt shrinks
but stays positive, reverse debt exactly cancels (→ remove), reverse debt is
overtaken (→ flip direction).

### Q: `ExactSplit` bug — assigning full amount to every user instead of per-user amounts.
**Gap filled:** an early implementation did
`for (User u : userList) result.put(u, amount)` — assigning the **entire**
expense total to every participant (3 people splitting ₹300 "exactly" would
each be charged ₹300 — ₹900 total, clearly wrong). Root cause: it never
read from `inputValues` at all, and iterated the group's full member list
instead of the actual input map's keys. Fixed by iterating `inputValues`
directly and validating `sum(inputValues.values()) == expense.getAmount()`
before accepting it.

### Q: `EqualSplit`/`PercentageSplit` — `BigDecimal.divide()` without scale/RoundingMode.
**Gap filled:** `BigDecimal.divide()` with no scale or `RoundingMode` throws
`ArithmeticException` whenever the division doesn't terminate exactly (e.g.,
₹100 ÷ 3). Beyond just adding a `RoundingMode`, the deeper rounding problem
(₹33.33 × 3 = ₹99.99, short by 1 cent) needed explicit remainder handling:
compute the per-person share at fixed scale, multiply back out, find the
leftover, and assign it deterministically (first participant in the list
absorbs it).

### Q: How does `editExpense()` correctly update the ledger, step by step?
**Gap filled with a full numeric trace** (Vijay pays ₹900 → later edited to
₹1200, both equally split 3 ways):
1. **Reverse the old expense's effect**: for each old share, call
   `updateBalance(payer, debtor, -oldAmount)` — nets old debts back toward
   zero (fully removing entries if nothing else exists between those pairs).
2. **Update the expense's own fields** (`amount`, `splitType` if changed).
3. **Recompute new shares** via the correct strategy.
4. **Reapply new shares** — `updateBalance(payer, debtor, +newAmount)` for
   each non-payer participant.

Same pattern, simpler, for `deleteExpense()`: reverse the expense's stored
shares (negate and apply), then remove it from the group's expense list. No
full ledger recomputation from scratch is ever needed — only the specific
expense's deltas are touched, which is what makes the live-ledger approach
efficient.

### Q: Same two users in two different groups — should their balances combine?
**Gap filled — the most important architectural question of the round, and
the one most easily gotten wrong:**

**Answer: keep them separate. Do not auto-combine across groups.**

Reasoning: balances are scoped to the social/financial context they arose
in. "Ravi owes Vijay ₹500 for a trip" and "Vijay owes Ravi ₹300 for
groceries" are conceptually distinct obligations that happen to involve the
same two people — silently netting them into "Ravi owes Vijay ₹200 net"
makes an implicit product decision the users may not want (e.g., wanting to
settle the trip debt with trip-group members present, independent of
grocery money).

**Design implication:** `BalanceSheet` should be scoped **per group**
(`Map<Group, BalanceSheet>`), not global across a user's entire account. A
"combined view across all your groups" — which real Splitwise does offer —
is a **separate, read-time aggregation layer** built on top of the
per-group ledgers, summing net balances per person across groups purely for
**display**, while each group's ledger stays independently authoritative.
This mirrors the earlier live-ledger-vs-on-demand-simplification trade-off:
keep the source of truth granular and correct; compute aggregate/simplified
views lazily, only when asked.

---

## 4. Trade-off Summary

| Decision | Alternative Considered | Why Chosen | Trade-off Accepted |
|---|---|---|---|
| Live incremental `BalanceSheet` | Recompute all balances from every expense on every read | O(1)-ish updates and lookups; expensive graph work never happens unless needed | Must carefully reverse/reapply deltas on edit/delete instead of just recomputing |
| Debt simplification computed on-demand | Simplify (collapse chains) on every write | Writes (adding expenses) are far more frequent than "show me simplified balances" — avoid wasted work | Simplification algorithm (min-cash-flow / greedy matching) must be run fresh each time it's requested |
| Single-direction, netted ledger (`balances[debtor][creditor]`) | Store both directions redundantly | No dual-write sync risk; simpler single-lookup reads; enforces a normalized invariant | `updateBalance()` must handle three cases (shrink, cancel, flip) instead of one flat write |
| Strategy registry (`Map<SplitType, CalculateSplit>`) | `if/else` branch on `SplitType` inside the manager | Satisfies Open/Closed — new split types need no changes to `addExpense()` | One extra layer of indirection (map lookup) |
| `Expense.shares` always stores final absolute amounts | Store raw input (percentages, etc.) directly | Downstream code (`BalanceSheet`) always works with one homogeneous format | Each strategy must convert its own input format before returning |
| Per-group `BalanceSheet` scoping | One global ledger across all of a user's groups | Matches real-world social/financial context boundaries; avoids surprising auto-netting | "Combined balance across groups" becomes a separate read-time aggregation, not free |
| `BigDecimal` over `double` for money | `double` | Avoids floating-point precision errors in financial calculations | Requires explicit `RoundingMode` on every `divide()` call |

---

## 5. Deep-Dive Q&A Round (Post-Code Review)

**Q1 — Single strategy field, multiple split types added in sequence.**
Bug: the manager was locked to one strategy at construction; a second
expense of a different type would silently compute wrong shares with no
error. Fix: strategy registry (see above).

**Q2 — SOLID principle violated by the `if/else` branch.**
Open/Closed Principle — adding a new split type required modifying
existing, tested code instead of only adding new code.

**Q3 — `ExactSplit` bug walkthrough.**
Needed to iterate `inputValues` (not the group's full member list) and
validate the sum against the expense total before accepting it.

**Q4 — What does `Expense.shares` represent?**
Always final calculated absolute-currency shares, never raw
percentages/inputs — see the dedicated Q&A entry above.

**Q5 — Full numeric trace of `editExpense()`.**
Reverse old shares → update expense fields → recompute new shares via the
correct strategy → reapply new shares. Walked through with concrete numbers
(₹900 → ₹1200, 3-way equal split).

**Q6 — Why single-direction storage, precisely?**
Not primarily "prevents duplicate entries" (an underspecified answer) — the
precise reasons are: no dual-write sync risk, simpler single-lookup reads,
and enforcing the normalized-ledger invariant.

**Q7 — Same two users across two different groups — combine balances?**
Keep separate, scope `BalanceSheet` per group; offer any "combined view" as
a read-time aggregation layer on top, not a merged ledger. See dedicated
entry above — this was the most consequential open question of the round.

---

## 6. Final Code

### Enums

```java
package com.example.interviewQuestions.LLD.Code.SplitWise.Enums;

public enum SplitType {
    EXACT,
    EQUAL,
    PERCENTAGE
}
```

### User

```java
package com.example.interviewQuestions.LLD.Code.SplitWise;

public class User {
    private final int userId;
    private String userName;

    public User(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    // NOTE: override equals()/hashCode() based on userId — User is used as
    // a map key throughout BalanceSheet and Expense.shares. Without this,
    // two objects representing the same person are treated as different
    // keys by HashMap's default identity-based equality.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return userId == ((User) o).userId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(userId);
    }
}
```

### Group

```java
package com.example.interviewQuestions.LLD.Code.SplitWise;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private int groupId;
    private String groupName;
    private final List<User> userList;

    public Group(int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.userList = new ArrayList<>();
    }

    public void addUser(User user) { userList.add(user); }

    public int getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public List<User> getUserList() { return userList; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    // Same note as User: override equals()/hashCode() on groupId since
    // Group is used as a map key in SplitWiseManager.shareExpenses.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group)) return false;
        return groupId == ((Group) o).groupId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(groupId);
    }
}
```

### Expense

```java
package com.example.interviewQuestions.LLD.Code.SplitWise;

import com.example.interviewQuestions.LLD.Code.SplitWise.Enums.SplitType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Expense {
    private final int expenseId;
    private final User expensePaidBy;
    private BigDecimal amount;
    private SplitType splitType;
    // Always holds FINAL, absolute-currency shares — never raw percentages
    // or unvalidated raw input. Each CalculateSplit implementation
    // normalizes its own input format into this before returning.
    private final Map<User, BigDecimal> shares;

    public Expense(int expenseId, BigDecimal amount, SplitType splitType, User expensePaidBy) {
        this.expenseId = expenseId;
        this.amount = amount;
        this.splitType = splitType;
        this.expensePaidBy = expensePaidBy;
        this.shares = new HashMap<>();
    }

    public int getExpenseId() { return expenseId; }
    public SplitType getSplitType() { return splitType; }
    public BigDecimal getAmount() { return amount; }
    public Map<User, BigDecimal> getShares() { return shares; }
    public User getExpensePaidBy() { return expensePaidBy; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
}
```

### CalculateSplit (Strategy interface + implementations)

```java
package com.example.interviewQuestions.LLD.Code.SplitWise.Strategy;

import com.example.interviewQuestions.LLD.Code.SplitWise.Expense;
import com.example.interviewQuestions.LLD.Code.SplitWise.Group;
import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.util.Map;

public interface CalculateSplit {
    Map<User, BigDecimal> expenseCalculation(Expense expense, Group group, Map<User, BigDecimal> inputValues);
}
```

```java
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
```

```java
package com.example.interviewQuestions.LLD.Code.SplitWise.Strategy;

import com.example.interviewQuestions.LLD.Code.SplitWise.Expense;
import com.example.interviewQuestions.LLD.Code.SplitWise.Group;
import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ExactSplit implements CalculateSplit {
    @Override
    public Map<User, BigDecimal> expenseCalculation(Expense expense, Group group, Map<User, BigDecimal> inputValues) {
        BigDecimal amount = expense.getAmount();

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : inputValues.values()) sum = sum.add(v);

        if (sum.compareTo(amount) != 0) {
            throw new IllegalArgumentException("Exact amounts must sum to total expense amount");
        }
        return new HashMap<>(inputValues);
    }
}
```

```java
package com.example.interviewQuestions.LLD.Code.SplitWise.Strategy;

import com.example.interviewQuestions.LLD.Code.SplitWise.Expense;
import com.example.interviewQuestions.LLD.Code.SplitWise.Group;
import com.example.interviewQuestions.LLD.Code.SplitWise.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class PercentageSplit implements CalculateSplit {
    @Override
    public Map<User, BigDecimal> expenseCalculation(Expense expense, Group group, Map<User, BigDecimal> inputValues) {
        BigDecimal amount = expense.getAmount();
        BigDecimal totalPercentage = BigDecimal.ZERO;
        Map<User, BigDecimal> result = new HashMap<>();

        for (Map.Entry<User, BigDecimal> entry : inputValues.entrySet()) {
            User user = entry.getKey();
            BigDecimal percentage = entry.getValue();
            totalPercentage = totalPercentage.add(percentage);

            BigDecimal shareAmount = amount.multiply(percentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            result.put(user, shareAmount);
        }

        if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("Percentages must add up to 100");
        }
        return result;
    }
}
```

### BalanceSheet (the ledger)

```java
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
```

### SplitWiseManager

```java
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
    private final Map<Group, List<Expense>> shareExpenses;
    private final BalanceSheet balanceSheet;
    private final Map<SplitType, CalculateSplit> strategies; // registry — OCP-compliant

    public SplitWiseManager(BalanceSheet balanceSheet, Map<SplitType, CalculateSplit> strategies) {
        this.balanceSheet = balanceSheet;
        this.shareExpenses = new HashMap<>();
        this.strategies = strategies;
    }

    public void addExpense(Group group, Expense expense, Map<User, BigDecimal> inputValues) {
        shareExpenses.computeIfAbsent(group, k -> new ArrayList<>()).add(expense);

        CalculateSplit strategy = strategies.get(expense.getSplitType());
        Map<User, BigDecimal> shares = strategy.expenseCalculation(expense, group, inputValues);
        expense.getShares().putAll(shares);

        applyShares(expense.getExpensePaidBy(), shares, false);
    }

    public void editExpense(Group group, Expense oldExpense, BigDecimal newAmount,
                             SplitType newSplitType, Map<User, BigDecimal> newInputValues) {
        // 1. Reverse the old expense's effect on the ledger
        applyShares(oldExpense.getExpensePaidBy(), oldExpense.getShares(), true);

        // 2. Update the expense's own fields
        oldExpense.setAmount(newAmount);
        oldExpense.setSplitType(newSplitType);

        // 3. Recompute and apply the new split
        CalculateSplit strategy = strategies.get(newSplitType);
        Map<User, BigDecimal> newShares = strategy.expenseCalculation(oldExpense, group, newInputValues);
        oldExpense.getShares().clear();
        oldExpense.getShares().putAll(newShares);

        applyShares(oldExpense.getExpensePaidBy(), newShares, false);
    }

    public void deleteExpense(Group group, Expense expense) {
        List<Expense> expenses = shareExpenses.get(group);
        if (expenses == null) return;

        applyShares(expense.getExpensePaidBy(), expense.getShares(), true);
        expenses.remove(expense);
    }

    private void applyShares(User payer, Map<User, BigDecimal> shares, boolean reverse) {
        for (Map.Entry<User, BigDecimal> entry : shares.entrySet()) {
            User debtor = entry.getKey();
            BigDecimal amount = entry.getValue();
            if (!debtor.equals(payer)) {
                balanceSheet.updateBalance(payer, debtor, reverse ? amount.negate() : amount);
            }
        }
    }
}
```

### Wiring / Main

```java
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
```

---

## 7. Overall Evaluation Summary

| Round | Score | Key strength | Key gap |
|---|---|---|---|
| Round 1 — Elevator | 7/10 | Self-identified the LOOK fairness trade-off unprompted | Verbal-to-code gap on agreed fixes |
| Round 2 — Splitwise | 6/10 | Excellent, proactive requirements gathering; honest about difficulty | Same verbal-to-code gap, more pronounced; 2 of 7 deep-dive questions (Q4, Q7) left unanswered |

### Standing pattern across both rounds
Reasoning quality is consistently ahead of code output. The recurring fix:
**close every flagged bug before moving to the next question** — treat
interviewer feedback as a checklist, not a suggestion. Speed with unresolved
issues reads worse than slower, verified progress.

### Areas to keep drilling
1. **Apply agreed fixes immediately** — several bugs (typos, `ExactSplit`
   logic, missing `RoundingMode`) were named explicitly in one turn and
   still present in the next submission.
2. **Never leave a question unanswered**, even under uncertainty — a
   reasoned wrong answer is scored higher than silence (Q4, Q7 this round).
3. **Justify design choices with the precise mechanism**, not just the
   correct-sounding conclusion — e.g., "prevents dual-write sync risk" beats
   "prevents duplicate entries" for the single-direction ledger question.
4. **Cost/trade-off framing is a strength — use it more.** The live-ledger
   vs. recompute-from-scratch, and per-group vs. global balance-sheet
   answers were both strong once reasoned through carefully; apply that same
   rigor as a default habit, not just when explicitly prompted.