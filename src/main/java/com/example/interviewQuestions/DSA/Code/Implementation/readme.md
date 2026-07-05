# LRU (Least Recently Used) Cache - Interview Notes

## Problem Statement

Design a data structure that supports the following operations in **O(1)** average time:

- `get(key)` → Return the value if the key exists; otherwise return `-1`.
- `put(key, value)` → Insert or update a key-value pair. If the cache exceeds its capacity, remove the **Least Recently Used (LRU)** item.

---

# Approach

An efficient LRU Cache uses **two data structures**:

1. **HashMap**
2. **Doubly Linked List**

## Why HashMap?

A HashMap provides **O(1)** lookup.

```java
Map<Integer, Node> map;
```

Instead of storing:

```java
Map<Integer, Integer>
```

we store:

```java
Map<Integer, Node>
```

because we need direct access to the corresponding node in the linked list.

Example:

```
Map

1 -> Node(1,10)
2 -> Node(2,20)
3 -> Node(3,30)
```

---

## Why Doubly Linked List?

A Doubly Linked List helps maintain the usage order.

```
Head (MRU)

3 <-> 2 <-> 1

Tail (LRU)
```

We can:

- Insert at front → O(1)
- Remove any node → O(1)
- Remove tail → O(1)

---

# Why not Singly Linked List?

Suppose we want to remove node `3`.

```
1 -> 2 -> 3 -> 4
```

To remove `3`, we need access to node `2`.

With a singly linked list, we don't know the previous node.

Therefore, we'd have to traverse the list.

Time Complexity:

```
O(n)
```

A doubly linked list stores:

- prev
- next

making removal an O(1) operation.

---

# Node Structure

```java
class Node {

    int key;
    int value;

    Node prev;
    Node next;
}
```

Each node stores exactly one key-value pair.

---

# Dummy Head and Tail

Instead of handling null pointers repeatedly, create two dummy nodes.

```
Head(-1,-1) <-> Tail(-1,-1)
```

After inserting key `1`

```
Head <-> 1 <-> Tail
```

After inserting key `2`

```
Head <-> 2 <-> 1 <-> Tail
```

Advantages:

- Eliminates edge cases.
- Avoids frequent null checks.
- Simplifies insertion and deletion logic.

---

# HashMap + Doubly Linked List

```
Map

1 -> Node1
2 -> Node2
3 -> Node3


Linked List

Head

3 <-> 2 <-> 1

Tail
```

Notice:

The **HashMap does not maintain order.**

Only the linked list maintains MRU/LRU ordering.

---

# Helper Functions

A clean implementation usually consists of four helper methods.

```java
addToFront(Node node)
```

Insert a node immediately after the dummy head.

---

```java
removeNode(Node node)
```

Detach a node from the linked list.

---

```java
moveToFront(Node node)
```

Equivalent to

```text
removeNode(node)
addToFront(node)
```

---

```java
removeTail()
```

Remove the least recently used node.

---

# get(key)

Algorithm

```
if key not present
    return -1

node = map.get(key)

moveToFront(node)

return node.value
```

Time Complexity

```
O(1)
```

---

# put(key, value)

### Case 1

Key already exists.

```
Update value

Move node to front

Return
```

---

### Case 2

Key does not exist.

If cache is full

```
Remove tail

Remove key from HashMap
```

Then

```
Create node

Insert at front

Insert into HashMap
```

---

# Dry Run

Capacity = 2

### put(1,1)

```
Head <-> 1 <-> Tail
```

Map

```
1 -> Node1
```

---

### put(2,2)

```
Head <-> 2 <-> 1 <-> Tail
```

Map

```
1 -> Node1
2 -> Node2
```

---

### get(1)

Move node 1 to front.

```
Head <-> 1 <-> 2 <-> Tail
```

MRU = 1

LRU = 2

---

### put(3,3)

Cache full.

Remove tail.

```
Head <-> 1 <-> Tail
```

Remove key

```
2
```

Insert 3.

```
Head <-> 3 <-> 1 <-> Tail
```

---

# Time Complexity

| Operation | Complexity |
|------------|------------|
| get | O(1) |
| put | O(1) |
| removeNode | O(1) |
| addToFront | O(1) |
| moveToFront | O(1) |
| removeTail | O(1) |

---

# Space Complexity

HashMap

```
O(capacity)
```

Linked List

```
O(capacity)
```

Overall

```
O(capacity)
```

---

# Common Interview Questions

## Why Map<Integer, Node> instead of Map<Integer, Integer>?

Suppose

```java
Map<Integer,Integer>
```

contains

```
1 -> 10
2 -> 20
```

Calling

```
get(2)
```

returns

```
20
```

But where is node `2` inside the linked list?

We don't know.

We must traverse the list.

```
Head

3 -> 5 -> 2 -> 8
```

Traversal

```
O(n)
```

Instead,

```
Map<Integer,Node>
```

stores the address of the node directly.

Hence removal and insertion remain **O(1)**.

---

## Why Dummy Nodes?

Without dummy nodes, we repeatedly write

```java
if(head == null)
```

```java
if(node == head)
```

```java
if(node == tail)
```

Dummy nodes eliminate these edge cases.

---

## What happens if HashMap is removed?

The cache still works using only a doubly linked list.

However,

Searching for a key requires traversal.

Therefore

```
get() = O(n)

put() = O(n)
```

because we must first determine whether the key already exists.

---

## Capacity = 1

Operations

```
put(1,1)

put(2,2)
```

Steps

```
Head <-> 1 <-> Tail
```

Remove `1`

```
Head <-> Tail
```

Insert `2`

```
Head <-> 2 <-> Tail
```

---

## Duplicate Key

Operations

```
put(1,10)

put(1,20)
```

Correct behavior

- Update value
- Move node to front
- Do **not** create another node

Incorrect behavior creates

```
Head

(1,20)

(1,10)

Tail
```

which is wrong.

---

## Why not ArrayList?

Removing from the middle

```
1 2 3 4 5
```

requires shifting elements.

Time Complexity

```
O(n)
```

Hence an ArrayList is not suitable.

---

## Thread Safety

The standard implementation is **not thread-safe**.

Two concurrent threads modifying the linked list can corrupt pointers.

Possible solutions:

- synchronized methods
- ReentrantLock
- ConcurrentHashMap + explicit locking for the linked list

---

# LFU vs LRU

## LRU

Removes the **Least Recently Used** node.

Uses

- HashMap
- Doubly Linked List

---

## LFU

Removes the **Least Frequently Used** node.

Each node additionally stores

```java
int frequency;
```

Common implementation

```
HashMap<Key, Node>

HashMap<Frequency, DoublyLinkedList>
```

Still supports O(1) average operations.

---

# Interview Takeaways

- Use **HashMap + Doubly Linked List**.
- Store **Node references** in the HashMap.
- Insert newly accessed nodes at the **front (MRU)**.
- Evict nodes from the **tail (LRU)**.
- Dummy nodes simplify implementation.
- All primary operations run in **O(1)** average time.