# DSA Mock Interview — Full Answer Key

Covers: Sliding Window, Graph, Linked List, Binary Tree, Stack Design, and Two Pointer.

---

## 1. Minimum Window Substring (Sliding Window)

**Problem:** Given `s` and `t`, find the smallest substring of `s` containing every character of `t` (with multiplicity).

### Intuition
Expand a window with pointer `j` until it contains everything `t` needs, then greedily shrink from the left with pointer `i` to find the smallest such window. The key insight: don't conflate "character belongs to `t`" with "we still need more of it" — duplicates break naive counting. Track two things:
- `need`: required count per character.
- `window`: current count per character in `[i, j]`.
- `formed`: how many **distinct characters** currently meet their required count.

When `formed == required` (number of distinct chars in `t`), the window is valid — shrink and record.

### Code (Java)
```java
class Solution {
    public String minWindow(String s, String t) {
        if (s.length() < t.length() || t.isEmpty()) return "";

        Map<Character, Integer> need = new HashMap<>();
        for (char c : t.toCharArray()) need.put(c, need.getOrDefault(c, 0) + 1);

        int required = need.size();
        int formed = 0;
        Map<Character, Integer> window = new HashMap<>();

        int i = 0, minLen = Integer.MAX_VALUE, minStart = 0;

        for (int j = 0; j < s.length(); j++) {
            char c = s.charAt(j);
            window.put(c, window.getOrDefault(c, 0) + 1);

            if (need.containsKey(c) && window.get(c).intValue() == need.get(c).intValue()) {
                formed++;
            }

            while (formed == required) {
                if (j - i + 1 < minLen) {
                    minLen = j - i + 1;
                    minStart = i;
                }
                char leftChar = s.charAt(i);
                window.put(leftChar, window.get(leftChar) - 1);
                if (need.containsKey(leftChar) && window.get(leftChar) < need.get(leftChar)) {
                    formed--;
                }
                i++;
            }
        }
        return minLen == Integer.MAX_VALUE ? "" : s.substring(minStart, minStart + minLen);
    }
}
```

### Tradeoffs
- **Time:** O(|s| + |t|) — amortized O(n); `i` and `j` each traverse `s` at most once.
- **Space:** O(|t|) for the maps (bounded by alphabet size, not input size).
- **Alternative (array instead of HashMap):** if the character set is known (e.g., ASCII/lowercase-only), a fixed-size `int[128]` array beats HashMap on constant factors — same complexity, faster in practice.

---

## 2. Detect & Return a Cycle in a Directed Graph

**Problem:** Given a directed graph, detect a cycle and return the actual node sequence forming it (not just yes/no).

### Intuition
**Kahn's algorithm (BFS + in-degree) detects existence but cannot reconstruct the path.** It tells you *which* nodes are "stuck" (never reach in-degree 0) but gives zero information about the *order* in which edges connect them — that's a set, not a sequence. Counterexample: `edges=[[0,2],[2,1],[1,0]]` — no node ever starts at in-degree 0, so the leftover set is `{0,1,2}` with no ordering info, and printing them in index order (`[0,1,2]`) is wrong since the real edges are `0→2→1→0`.

**DFS with 3-coloring gives you the path for free:**
- `WHITE` = unvisited, `GRAY` = on the current recursion stack (active path), `BLACK` = fully processed, safe.
- Maintain a `path` list mirroring the recursion stack.
- If DFS reaches a `GRAY` node, you've found a back-edge onto your own current path — the cycle is the slice of `path` from that node's index to the end.

### Code (Java)
```java
class Solution {
    List<List<Integer>> adj;
    int[] color; // 0=WHITE, 1=GRAY, 2=BLACK
    List<Integer> path = new ArrayList<>();
    List<Integer> cycle = null;

    public List<Integer> findCycle(int n, int[][] edges) {
        adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(e[1]);
        color = new int[n];

        for (int i = 0; i < n && cycle == null; i++) {
            if (color[i] == 0) dfs(i);
        }
        return cycle == null ? new ArrayList<>() : cycle;
    }

    private void dfs(int node) {
        if (cycle != null) return;
        color[node] = 1; // GRAY
        path.add(node);

        for (int next : adj.get(node)) {
            if (color[next] == 1) {
                // found back-edge -> extract cycle
                int start = path.indexOf(next);
                cycle = new ArrayList<>(path.subList(start, path.size()));
                return;
            }
            if (color[next] == 0) {
                dfs(next);
                if (cycle != null) return;
            }
        }

        path.remove(path.size() - 1);
        color[node] = 2; // BLACK
    }
}
```

### Tradeoffs
- **Time:** O(V + E) each node/edge visited once in the worst case (the `path.indexOf` call is O(V) but only triggered once, when the cycle is found, not per edge).
- **Space:** O(V) for color array, recursion stack, and path list.
- **Why not Kahn's + patch-up:** you could run Kahn's, then DFS *only* on the leftover in-degree>0 subgraph to recover order — but that's strictly more work (two algorithms) for the same result. DFS alone is both necessary and sufficient here.
- 
- **Recursion depth risk:** for very large/deep graphs (V ~ 10^5+), recursive DFS can stack-overflow; convert to an explicit stack-based DFS if that's a real constraint.

---

## 3. Odd-Even Linked List
**Problem:** Reorder a singly linked list so all odd-indexed nodes (1-based) come before all even-indexed nodes, preserving relative order within each group. O(1) space, O(n) time.

### Intuition
Use three pointers: `odd`, `even`, and `evenHead` (saved once, before the list gets rewired, since you'll need to reattach the even chain at the very end and `even`'s original position will otherwise be lost). Walk `odd` and `even` in lockstep, splicing each one two nodes ahead (skipping over the other group), advancing **both** pointers every iteration. The most common bug: advancing `odd` but forgetting to advance `even` — this desyncs the pointers and corrupts the list.

### Code (Java)
```java
class Solution {
    public ListNode oddEvenList(ListNode head) {
        if (head == null || head.next == null) return head;

        Node odd = head;
        Node even = head.next;
        Node evenHead = even;

        while (even != null && even.next != null) {
            odd.next = even.next;
            odd = odd.next;

            even.next = odd.next;
            even = even.next;
        }

        odd.next = evenHead;
        return head;
    }
}
```

### Tradeoffs
- **Time:** O(n), single pass.
- **Space:** O(1) — pure pointer rewiring, no extra nodes or lists.
- **Edge cases:** empty list and single-node list are handled by the early return. For odd-length lists, `even` naturally becomes `null` and the loop stops cleanly with `odd` on the last node; for even-length lists, `even.next` becomes `null` at the last valid iteration, and the original list's tail already terminates the even chain — no dangling reference, since you never point anything *past* the last real node except the final `odd.next = evenHead` splice.
- **Common bug (seen in this session):** forgetting `even = even.next;` — freezes `even` on one node forever, causing either an infinite loop or a corrupted list.

---

## 4. Binary Tree Maximum Path Sum

**Problem:** Find the maximum sum of any path in a binary tree (path doesn't need to pass through root, each node used at most once).

### Intuition
A "path" is a simple chain — at any node, it can use **at most two** of its three possible connections (left child, right child, parent). This creates two distinct concepts that must not be conflated:
1. **What a node returns to its parent** — the best *extendable* single-branch sum (`node.val + max(leftGain, rightGain)`), since the parent can only continue the path in one direction.
2. **What updates the global answer** — the best path *through* this node, allowed to use both branches (`node.val + leftGain + rightGain`), since a path ending here doesn't need to extend further.

Negative subtree sums are clamped to 0 via `Math.max(0, ...)` — a negative contribution should simply be excluded rather than dragging the sum down.

### Code (Java)
```java
class Solution {
    int ans = Integer.MIN_VALUE;

    public int maxPathSum(TreeNode root) {
        helper(root);
        return ans;
    }

    private int helper(TreeNode root) {
        if (root == null) return 0;

        int leftVal = Math.max(0, helper(root.left));
        int rightVal = Math.max(0, helper(root.right));

        ans = Math.max(ans, root.val + leftVal + rightVal); // path THROUGH this node

        return root.val + Math.max(leftVal, rightVal); // best branch to extend UPWARD
    }
}
```

### Tradeoffs
- **Time:** O(n) — every node visited exactly once.
- **Space:** O(h) recursion stack, where `h` is tree height (O(log n) balanced, O(n) skewed/degenerate).
- **Why returning `left + right` to the parent is wrong:** it would implicitly tell the parent "this subtree offers a path with two open ends," but the parent can only attach one edge — describing a node with three live connections, which isn't a valid path shape.

---

## 5. Queue Using Two Stacks

**Problem:** Implement FIFO queue operations (`push`, `pop`, `peek`, `empty`) using two stacks.

### Intuition
A stack reverses order once; reversing twice restores original order. Use two stacks:
- **`inStack`**: absorbs all incoming `push` calls directly (cheap, O(1)).
- **`outStack`**: used for `pop`/`peek`. If it's empty, dump all of `inStack` into it — this reversal makes the oldest element land on top of `outStack`, giving FIFO behavior.

The trick is *when* to transfer: only when `outStack` is empty. This means each element gets moved from `inStack` to `outStack` **at most once** in its lifetime, which is what makes the amortized cost O(1) despite individual transfers costing O(n).

### Code (Java)
```java
class MyQueue {
    private Stack<Integer> inStack = new Stack<>();
    private Stack<Integer> outStack = new Stack<>();

    public void push(int x) {
        inStack.push(x);
    }

    public int pop() {
        transferIfNeeded();
        return outStack.pop();
    }

    public int peek() {
        transferIfNeeded();
        return outStack.peek();
    }

    public boolean empty() {
        return inStack.isEmpty() && outStack.isEmpty();
    }

    private void transferIfNeeded() {
        if (outStack.isEmpty()) {
            while (!inStack.isEmpty()) {
                outStack.push(inStack.pop());
            }
        }
    }
}
```

### Tradeoffs
- **Time:** `push` is always O(1). `pop`/`peek` are O(1) **amortized** — worst case O(n) on a single call (when a transfer happens), but each element is moved from `inStack` to `outStack` exactly once across its entire lifetime, so total work over `n` operations is O(n), averaging to O(1) per call.
- **Space:** O(n) total across both stacks.
- **Why amortized analysis matters here:** a naive worst-case reading ("pop is O(n)!") is technically true per-call but misleading — the *aggregate* cost over any sequence of operations is linear, which is the honest way to characterize this structure's performance.

---

## 6. Min Stack (O(1) getMin)

**Problem:** Design a stack supporting `push`, `pop`, `top`, and `getMin`, all O(1).

### Intuition
Every stack frame carries a **snapshot of the minimum as of that point in time**. When you push a new value, you also compute and store `min(newValue, currentMin)` alongside it. Popping automatically "restores" the previous minimum because it was frozen into the frame below — no scanning needed, ever.

### Code (Java)
```java
class MinStack {
    private Stack<int[]> st = new Stack<>(); // {value, minSoFar}

    public MinStack() {
        st = new Stack<>();
    }

    public void push(int x) {
        int mini = st.isEmpty() ? x : Math.min(x, st.peek()[1]);
        st.push(new int[]{x, mini});
    }

    public void pop() {
        if (!st.isEmpty()) st.pop();
    }

    public int top() {
        return st.peek()[0];
    }

    public int getMin() {
        return st.peek()[1];
    }
}
```

### Tradeoffs
- **Time:** O(1) for all four operations — no scanning ever required.
- **Space:** O(n), but with a **2x constant factor** over a plain stack, since each frame stores a pair instead of a single value. This is the real cost of O(1) `getMin()`.
- **Alternative — two separate stacks:** a main data stack plus a second "min stack" that only pushes a new minimum when a new value is ≤ current min (and only pops when the popped value equals the current min). Same time complexity, but slightly better space in the common case (min stack doesn't grow on every push) — though worst case (strictly decreasing input) it still grows to size n, so asymptotically identical.

---

## 7. Count Pairs Summing to K (Two Pointer, Duplicates)

**Problem:** Given a sorted array, count index-pairs `(i, j)`, `i < j`, where `nums[i] + nums[j] == k`. Handle duplicate values correctly (each pair of *indices* counts once).

### Intuition
Standard two-pointer converge/diverge based on sum vs. `k` — but naive "found a match, count++, move both pointers by 1" **silently drops pairs when duplicates exist on either side**. Instead, when a match is found, process the entire matching **block** combinatorially:

- **If `nums[i] == nums[j]`** (meaning the whole window between them is one repeated value, since the array is sorted): the block size is `m = j - i + 1`; the count of valid pairs is "choose 2 from m identical items" = `m*(m-1)/2`. Since every pair in this window sums to `2*nums[i] = k`, this is the entire remaining answer — done.
- **If `nums[i] != nums[j]`:** count `countI` = how many times `nums[i]` repeats forward, `countJ` = how many times `nums[j]` repeats backward. Every element in the i-block pairs validly with every element in the j-block, giving `countI * countJ` pairs. Then advance `i` and `j` **one step past** their respective blocks (not "jump by countI/countJ" — that double-counts the block traversal already done while counting duplicates).

### Code (Java)
```java
class Solution {
    public int countPairs(int[] nums, int k) {
        int n = nums.length;
        int i = 0, j = n - 1, ans = 0;

        while (i < j) {
            int sum = nums[i] + nums[j];

            if (sum == k) {
                if (nums[i] == nums[j]) {
                    int m = j - i + 1;
                    ans += (m * (m - 1)) / 2;
                    break; // entire remaining window is one value; done
                } else {
                    int countI = 1, countJ = 1;
                    while (i + 1 < n && nums[i] == nums[i + 1]) { i++; countI++; }
                    while (j - 1 >= 0 && nums[j] == nums[j - 1]) { j--; countJ++; }
                    ans += countI * countJ;
                    i++; // step ONE past the block just consumed
                    j--;
                }
            } else if (sum < k) {
                i++;
            } else {
                j--;
            }
        }
        return ans;
    }
}
```

### Tradeoffs
- **Time:** O(n) — `i` and `j` each move strictly forward/backward across the array; even with the inner while loops counting duplicates, total work across all iterations is bounded by n (each index visited a constant number of times).
- **Space:** O(1) — no auxiliary maps, unlike a hashmap-based two-sum-count approach.
- **Why not a HashMap approach:** a frequency-map + complement-lookup solution also solves this correctly (and is arguably easier to get right on duplicates) but costs O(n) space. Two-pointer is preferred specifically *because the array is sorted* — sortedness is what lets you avoid the extra space. If the array weren't sorted, sorting first costs O(n log n), which may or may not still beat the hashmap approach depending on constraints.
- **Bug pattern to watch for (hit 3 times in this session):** after counting a duplicate block with an inner while loop, don't re-add `countI`/`countJ` to jump the outer pointers — the inner loop already advanced them to the last element of the block. Only one more step (`i++`, `j--`) is needed to move past it.

---

## Summary Table

| Problem | Technique | Time | Space |
|---|---|---|---|
| Min Window Substring | Sliding window + 2 freq maps | O(\|s\|+\|t\|) | O(\|t\|) |
| Cycle Detection & Reconstruction | DFS + 3-coloring | O(V+E) | O(V) |
| Odd-Even Linked List | 3-pointer in-place rewiring | O(n) | O(1) |
| Max Path Sum | Post-order recursion, dual return values | O(n) | O(h) |
| Queue via 2 Stacks | Lazy transfer, amortized analysis | O(1) amortized | O(n) |
| Min Stack | Stack of (value, minSoFar) pairs | O(1) | O(n), 2x constant |
| Pair Count with Duplicates | Two pointer + block combinatorics | O(n) | O(1) |
