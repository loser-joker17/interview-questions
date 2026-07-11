# DSA Mock Interview — Round 2: Questions, Mistakes & Resolutions

Advanced (SDE-2) level. 5 problems across Two Pointer/Sliding Window, Stacks, Binary Tree, Graph, and Linked List.

---

## 1. Shortest Subarray with Sum ≥ Target (Two Pointer / Sliding Window)

### Question
> Given an array of positive integers `nums` and a positive integer `target`, find the length of the **shortest** contiguous subarray whose sum is ≥ `target`. Return `0` if no such subarray exists.
>
> Example: `nums = [2,3,1,2,4,3]`, `target = 7` → output: `2` (subarray `[4,3]`)

### What went wrong
Nothing structural — the core two-pointer logic (expand with `j`, shrink with `i` while `sum ≥ target`, track min window) was correct on the **first attempt**. The only gap: the initial code printed `ans` directly, which would print `Integer.MAX_VALUE` instead of `0` when no valid subarray exists (e.g., `nums=[1,1,1,1], target=100`).

### Resolution
Added a check: if `ans == Integer.MAX_VALUE` after the loop, output `0` instead.

### Correct Code (Java)
```java
import java.util.*;

class Codechef {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = sc.nextInt();
        int target = sc.nextInt();

        int i = 0, j = 0, ans = Integer.MAX_VALUE, sum = 0;

        while (j < n) {
            sum += arr[j];

            while (i < n && sum >= target) {
                ans = Math.min(ans, j - i + 1);
                sum -= arr[i];
                i++;
            }
            j++;
        }

        System.out.println(ans == Integer.MAX_VALUE ? 0 : ans);
    }
}
```

### Complexity
- **Time:** O(n) — `i` and `j` each traverse the array at most once (amortized two-pointer argument).
- **Space:** O(1).
- **Key assumption:** array must contain **only positive integers** — sum is monotonic as the window grows, which is what makes "shrink while still valid" correct. With negative numbers present, this breaks and a different technique (prefix sums + monotonic deque) is needed.

---

## 2. Valid Parentheses (Stack)

### Question
> Given a string containing only `(`, `)`, `{`, `}`, `[`, `]`, determine if it's valid — every open bracket must be closed by the same type, in the correct order.
>
> Example: `"{[()]}"` → `true`, `"{[(])}"` → `false`

### What went wrong
First attempt used a **counting approach** — three separate counters for each bracket type, incrementing on open and decrementing on close, declaring the string valid if all counters returned to zero. This is fundamentally broken: it only tracks **quantity**, not **order**. Counterexample: `"([)]"` — all counts end at zero (1 open/close each of `(` and `[`), so the counting approach says `true`, but the actual order is wrong (`)` tries to close when `[` was the most recently opened bracket, not `(`).

### Resolution
Switched to a stack-based approach: push opening brackets; on a closing bracket, check the stack isn't empty and that `peek()` is the matching opener before popping. Two distinct failure modes: (1) closing bracket arrives with an empty stack, (2) closing bracket arrives but the top of the stack doesn't match its type.

### Correct Code (Java)
```java
import java.util.*;

class Solution {
    public boolean isValid(String s) {
        Stack<Character> st = new Stack<>();
        int n = s.length();

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                st.push(c);
            } else {
                if (st.empty()) return false;

                if (c == ')') {
                    if (st.peek() == '(') st.pop(); else return false;
                } else if (c == ']') {
                    if (st.peek() == '[') st.pop(); else return false;
                } else if (c == '}') {
                    if (st.peek() == '{') st.pop(); else return false;
                }
            }
        }

        return st.empty();
    }
}
```

### Complexity
- **Time:** O(n).
- **Space:** O(n) worst case (all opening brackets, e.g. `"((((("`).
- **Why the stack matters:** the stack's "top" naturally always represents "most recently opened, not yet closed" bracket — exactly the ordering information the counting approach threw away.
- **Cleaner variant:** replace the three `if` blocks with a `Map<Character,Character>` from closer → expected opener, collapsing the type-check into one lookup. Functionally identical, less repetitive.

---

## 3. Serialize and Deserialize Binary Tree

### Question
> Design `serialize(TreeNode root) -> String` and `deserialize(String data) -> TreeNode` such that `deserialize(serialize(root))` reconstructs a tree **identical** to the original, including `null` positions. A plain traversal without null markers can't uniquely reconstruct an arbitrary tree.

### What went wrong
**First pass: skipped entirely** ("I don't know" → asked to move to the next question without attempting reasoning). On retry, the answer was strong: correctly identified that preorder + explicit `#` null-markers solves the ambiguity problem, and — unprompted — correctly explained *why inorder specifically fails* even with null markers (inorder doesn't visit the root first, so you can't identify the root position from the string alone; preorder/postorder both visit root at a fixed, predictable position).

The only implementation bug: the `Codec` class used an instance variable `idx` to track position during deserialization, but never reset it to `0` at the start of `deserialize()`. This is a **statefulness bug** — calling `deserialize()` a second time on the same `Codec` instance would silently read from the wrong position (leftover from the previous call) or throw an `ArrayIndexOutOfBoundsException`.

### Resolution
Reset `idx = 0;` at the start of `deserialize()`, mirroring how `str = new StringBuilder();` is freshly reset at the start of `serialize()`.

### Correct Code (Java)
```java
public class Codec {
    StringBuilder str;

    public String serialize(TreeNode root) {
        str = new StringBuilder();
        helper(root);
        return str.toString();
    }

    private void helper(TreeNode root) {
        if (root == null) {
            str.append("#,");
            return;
        }
        str.append(root.val).append(",");
        helper(root.left);
        helper(root.right);
    }

    int idx;

    public TreeNode deserialize(String data) {
        idx = 0; // FIX: reset on every call, not just once at construction
        String[] arr = data.split(",");
        return generateBinaryTree(arr);
    }

    private TreeNode generateBinaryTree(String[] arr) {
        if (arr[idx].equals("#")) {
            idx++;
            return null;
        }
        TreeNode root = new TreeNode(Integer.parseInt(arr[idx]));
        idx++;
        root.left = generateBinaryTree(arr);
        root.right = generateBinaryTree(arr);
        return root;
    }
}
```

### Complexity
- **Time:** O(n) for both serialize and deserialize — each node visited exactly once.
- **Space:** O(n) for the string/array, plus O(h) recursion stack.
- **Why preorder:** it visits `root → left → right`, matching how deserialization naturally rebuilds top-down — the root's value is always the very next token needed. Inorder doesn't give you the root first, so you can't bootstrap the recursive rebuild from it alone.

---

## 4. Number of Islands (Graph / DFS)

### Question
> Given an `m x n` grid of `0`s (water) and `1`s (land), count the number of distinct islands (groups of `1`s connected 4-directionally).
>
> ```
> 11000
> 11000
> 00100
> 00011
> ```
> Output: `3`

### What went wrong
The overall approach (DFS flood-fill, count how many times a *new* traversal is initiated) was right from the start, but the initial base-case check for the DFS recursion was buggy:
```
if (x<0 || y<0 || x>n || y>m) return 0;
```
Two problems: (1) **off-by-one** — for an `n`-row grid, valid indices are `0` to `n-1`, so `x>n` incorrectly allows `x==n` (out of bounds); should be `x>=n`. (2) **missing guards** — no check for water cells (`grid[x][y]==0`) or already-visited cells, meaning DFS would recurse into water and infinitely re-traverse counted land.

### Resolution
Combined all guards into the neighbor-check *before* recursing (rather than as a base case inside the function): bounds check with correct half-open range (`X<n && Y<m`), plus `grid[X][Y]=='1'` (is land) and `!visited[X][Y]` (not yet visited), all in one condition.

### Correct Code (C++)
```cpp
class Solution {
public:
    int dx[4] = {0, 1, 0, -1};
    int dy[4] = {-1, 0, 1, 0};

    void dfs(vector<vector<char>>& grid, int x, int y, int n, int m, vector<vector<bool>>& visited) {
        visited[x][y] = true;
        for (int i = 0; i < 4; i++) {
            int X = x + dx[i];
            int Y = y + dy[i];
            if (X >= 0 && Y >= 0 && X < n && Y < m &&
                grid[X][Y] == '1' && !visited[X][Y]) {
                dfs(grid, X, Y, n, m, visited);
            }
        }
    }

    int numIslands(vector<vector<char>>& grid) {
        int n = grid.size();
        int m = grid[0].size();
        vector<vector<bool>> visited(n, vector<bool>(m, false));
        int ans = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (grid[i][j] == '1' && !visited[i][j]) {
                    ans++;
                    dfs(grid, i, j, n, m, visited);
                }
            }
        }
        return ans;
    }
};
```

### Complexity
- **Time:** O(n×m) — every cell visited at most once across all DFS calls combined.
- **Space:** O(n×m) for the `visited` array (can be reduced to O(1) extra by mutating the grid in-place — set visited land to `'0'` — at the cost of destroying the input; worth explicitly flagging this tradeoff to an interviewer rather than doing it silently). Worst-case O(n×m) recursion depth for a grid that's a single large connected island — a real stack-overflow risk on large inputs; an iterative DFS with an explicit stack avoids this.

---

## 5. Linked List Cycle II — Find Where the Cycle Starts

### Question
> Given the head of a singly linked list, determine if it has a cycle. If it does, return the node where the cycle **begins**. Otherwise return `null`. Must use O(1) extra space.
>
> Example: `3 → 2 → 0 → -4 → (back to 2)` → output: node with value `2`

### What went wrong
Cycle **detection** via Floyd's (slow/fast pointers) was correct immediately. The harder half — finding *where* the cycle starts — initially stalled: the first attempt set `node1 = slow` and `node2 = fast`, which is meaningless since at the meeting point `slow == fast` already (both pointers are just the same node). A second issue: the algebraic justification for the two-pointer trick wasn't derived, just asserted.

Also, the initial `getCycleNode` returned `new Node(-1)` instead of `null` for the no-cycle case — a fabricated sentinel that could silently masquerade as a real value downstream instead of clearly signaling "no cycle."

### Resolution
Worked through the derivation: let `L1` = distance from head to cycle start, `L2` = distance from cycle start to the meeting point, `C` = cycle length. At the meeting point: `slow` traveled `L1+L2`, `fast` traveled `2*(L1+L2)`, and also `fast` traveled `L1+L2+k*C` for some integer `k` (extra full loops). Setting these equal and simplifying: `L1 = k*C - L2`, which proves that walking `L1` steps from the meeting point lands exactly back at the cycle start — the same distance a pointer starting at `head` needs to travel. This justifies initializing **one pointer at `head`** and **one pointer at the meeting point**, both advancing one step at a time, until they meet — that meeting point is the cycle's start. Also fixed the no-cycle return value to `null`.

### Correct Code (Java)
```java
class Node {
    int data;
    Node next;

    Node(int data) {
        this.data = data;
        this.next = null;
    }

    public static Node getCycleNode(Node head) {
        Node slow = head;
        Node fast = head;

        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;

            if (slow == fast) {
                Node node1 = slow; // stays at meeting point
                Node node2 = head; // starts at head

                while (node1 != node2) {
                    node1 = node1.next;
                    node2 = node2.next;
                }
                return node1; // cycle start
            }
        }
        return null; // FIX: was `new Node(-1)`
    }
}
```

### Complexity
- **Time:** O(n) — Floyd's phase is at most O(n), phase two is bounded by `L1 ≤ n`.
- **Space:** O(1) — a few pointers only. Strictly better than the alternative (hashset of visited nodes), which is correct but costs O(n) space.
- **Caller note:** since this can return `null`, any caller dereferencing the result (e.g. `.data`) needs a null check first, or it'll throw an NPE on cycle-free lists.

---

## Summary
**Common theme across mistakes:** most bugs were about **incomplete edge-case coverage** (empty results, statefulness across repeated calls, off-by-one bounds) rather than wrong overall algorithm choice — the high-level approach was usually right; execution details needed a second pass.
