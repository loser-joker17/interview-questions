# Elevator System — LLD Interview Notes

A record of the mock Low-Level Design interview: requirements, design decisions,
doubts raised and resolved, trade-offs, the LOOK algorithm, and final code.
---

## 1. Requirements Gathered

| Question | Decision |
|---|---|
| How many elevators? | Bank of N elevators (example used: 4). Design must not hardcode the count. |
| How many floors? | Fixed at design time (example: 20), but not hardcoded into logic. |
| Capacity/weight limit? | Yes — each elevator has a max passenger capacity, enforced before boarding. |
| Multiple destinations per person? | Yes, conceptually — but each floor press is modeled as one request. |
| Invalid requests? | Must be handled gracefully (e.g., non-existent floor, same-floor request) — no crashes. |
| Request types | Two distinct kinds: **external** (floor button, has direction, no elevator chosen yet) and **internal** (in-car button, destination only, elevator already fixed). |

---

## 2. Core Entities & Responsibilities

- **`Request`** (abstract) — common fields: `floor`, `timeStamp`, `requestState`.
- **`ExternalRequest`** — adds `Direction` (person hasn't boarded; system must decide *which* elevator).
- **`InternalRequest`** — destination floor only, no `Direction` (person already inside a specific elevator).
- **`Elevator`** — a single car: position, capacity, door/motion state, and its own pending stops.
- **`ElevatorManager`** — owns all elevators, decides **which** elevator gets an external request.
- **Enums** — `Direction`, `ElevatorState`, `DoorState`, `RequestState` (kept separate — see Q&A below).

---

## 3. Doubts Raised & Gaps Filled (Q&A Log)

### Q: Why not a simple FIFO queue for destinations?
**Doubt:** Initially proposed a `Queue<Pair<Direction, Floor>>`, processing requests in arrival order.

**Gap filled:** Arrival-order queues cause unnecessary backtracking. Example: elevator at floor 1,
requests arrive for floor 9 then floor 3 — FIFO visits `1 → 9 → 3`, even though 3 is on the way to 9.
Switched to **two sorted sets** (`upStops`, `downStops`) so stops are always served in floor order
along the current direction, regardless of arrival order.

### Q: Doesn't sorted-order serving still starve the "far" passenger (e.g., floor 12) while nearer stops (3,5,7,8) get served first?
**Doubt raised independently** — a genuinely good catch.

**Resolution:** Yes, and that's an accepted, intentional trade-off, not a bug. This is exactly why
elevators use **LOOK/SCAN**: it optimizes for **overall throughput and no-starvation guarantee**,
not for any single rider's latency. The far passenger waits longer but is *never* skipped — the
elevator only reverses once it exhausts all pending stops in the current direction. The alternative
(always reroute to the nearest request) causes worse problems: potential starvation of far requests,
unpredictable ETAs, and unnecessary direction reversals. This matches real elevator behavior.

### Q: Why `TreeSet<Integer>` instead of `TreeSet<Request>`?
**Gap filled:**
- Elevator movement only needs *which floors* to stop at — not who requested them.
- `TreeSet<Integer>` gives free de-duplication (3 people on floor 7 → one stop).
- Making `Request` sortable would need artificial `compareTo()`/`equals()` logic that just
  reduces to comparing floors anyway — extra complexity for no gain.
- **Trade-off accepted:** losing track of *which* individual `Request` objects are satisfied when a
  floor is reached. Resolved by keeping a separate `Map<Integer, List<Request>>` in the
  `ElevatorManager` for request-lifecycle tracking, while `Elevator` itself only tracks primitive floors.

### Q: Why not `PriorityQueue` or `LinkedList` instead of `TreeSet`?
**Gap filled:**
- `LinkedList`/`Queue` → no sorted order at all (back to the FIFO problem).
- `PriorityQueue` → O(log n) for peek/poll of the min, same as `TreeSet` for our use case,
  **but** lacks range queries (`ceiling()`, `floor()`) and doesn't natively deduplicate.
  `TreeSet` gives both for free, which is why it's preferred here — not primarily a Big-O argument.

| Operation | TreeSet | PriorityQueue |
|---|---|---|
| `add(floor)` | O(log n) | O(log n) |
| `remove(floor)` (arbitrary) | O(log n) | **O(n)** (linear scan unless removing the root) |
| `getNextStop` (`first()`/`peek()`) | O(log n) | O(1) |

### Q: Do two simultaneous same-floor same-direction requests (e.g., two people on floor 5 press UP) both get preserved?
**Gap filled — this is a modeling issue, not a concurrency issue** (true even single-threaded):
- The **elevator stop** correctly de-duplicates — floor 5 only needs to be visited once. That's desired.
- The **two individual `Request` objects** are a separate concern — if each needs to be marked
  `COMPLETED` independently (e.g., to notify two different people), that tracking must live outside
  the `TreeSet`, e.g., in the manager's per-floor request map.

### Q: Why does `ElevatorState` and `Direction` exist as separate enums instead of one merged `{IDLE, MOVING_UP, MOVING_DOWN}`?
**Gap filled:** They represent two **orthogonal axes** — operational status vs. direction of travel.
Merging them causes a **combinatorial explosion** the moment you add a new state. Adding
`MAINTENANCE` to the separate-enum design is a one-line change:
`ElevatorState { IDLE, MOVING, MAINTENANCE, OUT_OF_SERVICE }`. In the merged design, you'd need
direction-qualified nonsense like `MAINTENANCE_UP`/`MAINTENANCE_DOWN`, or accept inconsistent
semantics where some values imply direction and others don't — messy and harder to maintain.

### Q: Given two elevators with equal floor-distance to a pending request, do they take equally long?
**Gap filled — key miss to internalize:** **No.** Floor-distance alone is an insufficient cost metric.

Example: Elevator A (floor 2, moving UP, pending stops `{3,4,5,6,7}`) vs. Elevator B
(floor 8, IDLE) — both have `distance = 3` to a request at floor 5.
A must **stop and open/close its door at floors 3 and 4** before reaching 5 — real wall-clock time
B doesn't spend. **Improved cost function:**

```
cost = distance + (intermediateStopsCount * STOP_TIME_PENALTY)
```

### Q: Why does `addStop(floor)` compare against `currentFloor` instead of using the passenger's `Direction`?
**Gap filled — subtle but important distinction:**
- `ExternalRequest.direction` = *the passenger's intent after boarding* ("once I'm in, I want to go down").
- `addStop`'s `floor > currentFloor` check = *purely geometric*: is this stop on the elevator's
  physical path while sweeping up or down **to reach it**, regardless of what the passenger
  does afterward.

Concrete example: elevator at floor 2; someone on floor 6 presses **DOWN** (wants to end up lower,
e.g., floor 1). The elevator still must travel **upward** from 2 to 6 just to **pick them up** — so
`6 > currentFloor(2)` correctly places this stop in `upStops`, even though the passenger's stated
direction is DOWN. This is why `InternalRequest` (no `Direction` field at all) can reuse the exact
same `addStop` logic — movement math never needs to know the passenger's post-boarding intent.

### Q: Who handles an internal request — `Elevator`, `ElevatorManager`, or a `RequestDispatcher`?
**Gap filled:** The `Elevator` itself, directly (`elevator.addStop(floor)`) — **no manager involved.**
The `ElevatorManager`'s sole job is *assignment*: deciding which elevator should serve a request
when that's still undecided (true only for external requests). For an internal request, the
passenger has already physically chosen their elevator by boarding it — there's no assignment
decision left to make, so routing it through the manager is unnecessary indirection.

### Q: Is the implementation thread-safe? How to fix it without a global lock?
**Gap filled:**
- `ArrayList<Elevator>` → rarely mutated, read constantly → use `CopyOnWriteArrayList`.
- `TreeSet<Integer>` per elevator → swap for `ConcurrentSkipListSet<Integer>` (same sorted-set API,
  genuinely thread-safe, no manual locking).
- **Lock per-elevator, not globally** — two threads assigning to *different* elevators shouldn't
  block each other.
- **Named race condition:** in `assignRequest()`, two threads can both *read* Elevator X's cost
  (check) before either *commits* the assignment (act) — a classic check-then-act race that can
  double-assign the same elevator. Fix: make check-and-commit atomic per candidate (synchronize on
  that elevator during the read+add), or route each elevator's commands through a single-writer
  queue so its own state is never mutated concurrently.

### Q: Mid-sweep, a new request arrives — does the elevator react immediately?
**Gap filled:**
- Floor 6 presses **UP** while elevator sweeps 2→10 (already moving up): 6 is simply inserted into
  `upStops` and gets served automatically as the sweep continues — no special-casing needed.
- Floor 6 presses **DOWN** instead: goes into `downStops`, and is **deliberately not served** until
  the current up-sweep finishes and the elevator reverses — even though it's physically passing
  floor 6. This matches real-world elevator behavior (you've likely seen an elevator pass your floor
  without stopping because your call was opposite its current direction). This is precisely why
  `upStops`/`downStops` are kept as **two separate sets** rather than one merged set — the
  separation is what encodes LOOK's "never reverse mid-sweep" guarantee.

---

## 4. The LOOK Algorithm — Explained with an Example

**Rule:** keep moving in one direction, serving every pending stop along the way in sorted order;
only reverse once nothing remains ahead in that direction.

**Example:** Elevator starts at floor 1, `currentDirection = UPWARD`. Requests arrive, in this
arrival order: floor 9, floor 3, floor 5.

`addStop` sorts by position, not arrival order:
```
upStops   = {3, 5, 9}
downStops = {}
```

| Step | currentFloor | getNextStop() | Action |
|---|---|---|---|
| 1 | 1 | 3 | move to 2 |
| 2 | 2 | 3 | move to 3 → reached, open door, remove(3) → `{5,9}` |
| 3 | 3 | 5 | move to 4 |
| 4 | 4 | 5 | move to 5 → reached, remove(5) → `{9}` |
| 5 | 5 | 9 | move to 6 ... continues to 9, reached, remove(9) → `{}` |
| next | 9 | `-1` | both sets empty → `elevatorState = IDLE` |

Even though floor 9 was requested **first**, it's served **last** — nearest-in-current-direction
order wins over arrival order. This is the entire point of LOOK, and the deliberate trade-off behind
it: fairness/throughput in aggregate, not minimal latency for any one rider.

### Bug caught and fixed during the session
An early `getNextStop()` implementation flipped `currentDirection` **on every call**, instead of
only when the current direction's stop set was exhausted. This caused pending stops to be silently
stranded (e.g., reversing away from `upStops = {7,9}` after serving only floor 5). Corrected version:

```java
public int getNextStop() {
    if (currentDirection == Direction.UPWARD) {
        if (!upStops.isEmpty()) return upStops.first();
        if (!downStops.isEmpty()) {
            currentDirection = Direction.DOWNWARD; // reverse only when exhausted
            return downStops.first();
        }
    } else {
        if (!downStops.isEmpty()) return downStops.first();
        if (!upStops.isEmpty()) {
            currentDirection = Direction.UPWARD;
            return upStops.first();
        }
    }
    return -1; // nothing pending anywhere
}
```

---

## 5. Trade-off Summary

| Decision | Alternative Considered | Why Chosen | Trade-off Accepted |
|---|---|---|---|
| Two sorted `TreeSet`s (up/down) | FIFO `Queue` | Avoids inefficient/arrival-order routing | Slightly more complex than a single queue |
| LOOK/SCAN scheduling | Always-nearest-first | No starvation, matches real elevators, bounded wait | Not optimal for any single rider's latency |
| `TreeSet<Integer>` for stops | `TreeSet<Request>` | Decouples movement from request identity; free dedup | Must track individual `Request` lifecycle separately (in manager) |
| `TreeSet` over `PriorityQueue` | `PriorityQueue` | Dedup + range queries (`ceiling`/`floor`) | Marginal — both are O(log n) for core ops |
| Separate `ElevatorState`/`Direction` enums | Merged `{IDLE, MOVING_UP, MOVING_DOWN}` | Avoids combinatorial explosion when adding new states (e.g. `MAINTENANCE`) | One extra field to reason about |
| Cost = distance + stop penalty | Cost = raw distance | Reflects real wait time (door dwell time at intermediate stops) | Requires calibrating `STOP_TIME_PENALTY` |
| Internal requests go straight to `Elevator` | Route through `ElevatorManager` | No assignment decision needed — elevator already chosen | None significant |
| `ConcurrentSkipListSet` + per-elevator locks | Global `synchronized` on manager | Avoids serializing unrelated elevators through one lock | More moving parts to reason about |

---

## 6. Final Code

### Enums

```java
package com.example.interviewQuestions.LLD.Code.Elevator.State;

public enum Direction {
    UPWARD,
    DOWNWARD
}
```

```java
package com.example.interviewQuestions.LLD.Code.Elevator.State;

public enum DoorState {
    OPEN,
    CLOSE
}
```

```java
package com.example.interviewQuestions.LLD.Code.Elevator.State;

public enum ElevatorState {
    IDLE,
    MOVING
    // Extensible: MAINTENANCE, OUT_OF_SERVICE, etc. can be added without
    // touching 
}
```

```java
package com.example.interviewQuestions.LLD.Code.Elevator.State;

public enum RequestState {
    PENDING,
    ASSIGNED,
    COMPLETED
}
```

### Request Hierarchy

```java
package com.example.interviewQuestions.LLD.Code.Elevator;

import com.example.interviewQuestions.LLD.Code.Elevator.State.RequestState;

public abstract class Request {
    protected int floor;
    protected long timeStamp;
    protected RequestState requestState;

    public Request(int floor, long timeStamp) {
        this.floor = floor;
        this.timeStamp = timeStamp;
        this.requestState = RequestState.PENDING;
    }

    public int getFloor() { return floor; }
    public long getTimeStamp() { return timeStamp; }
    public RequestState getRequestState() { return requestState; }
    public void setRequestState(RequestState requestState) { this.requestState = requestState; }
}
```

```java
package com.example.interviewQuestions.LLD.Code.Elevator;

import com.example.interviewQuestions.LLD.Code.Elevator.State.Direction;

// Needs Direction: passenger hasn't boarded yet, so the system must know
// which way they intend to travel once picked up.
public class ExternalRequest extends Request {
    private Direction direction;

    public ExternalRequest(int floor, Direction direction, long timeStamp) {
        super(floor, timeStamp);
        this.direction = direction;
    }

    public Direction getDirection() { return direction; }
}
```

```java
package com.example.interviewQuestions.LLD.Code.Elevator;

// No Direction needed: passenger is already inside a specific elevator;
// direction is derivable from destination vs. currentFloor.
public class InternalRequest extends Request {
    public InternalRequest(int destinationFloor, long timeStamp) {
        super(destinationFloor, timeStamp);
    }
}
```

### Elevator

```java
package com.example.interviewQuestions.LLD.Code.Elevator;

import com.example.interviewQuestions.LLD.Code.Elevator.State.Direction;
import com.example.interviewQuestions.LLD.Code.Elevator.State.DoorState;
import com.example.interviewQuestions.LLD.Code.Elevator.State.ElevatorState;

import java.util.TreeSet;

public class Elevator {
    private final int elevatorId;
    private int currentFloor;
    private final int capacity;
    private int currentLoad;

    private final TreeSet<Integer> upStops;
    private final TreeSet<Integer> downStops;

    private ElevatorState elevatorState;
    private DoorState doorState;
    private Direction currentDirection;

    public Elevator(int elevatorId, int currentFloor, int capacity) {
        this.elevatorId = elevatorId;
        this.currentFloor = currentFloor;
        this.capacity = capacity;
        this.currentLoad = 0;

        this.upStops = new TreeSet<>();
        this.downStops = new TreeSet<>((a, b) -> b - a); // descending

        this.elevatorState = ElevatorState.IDLE;
        this.doorState = DoorState.CLOSE;
        this.currentDirection = Direction.UPWARD;
    }

    /** Purely geometric: is this floor above or below me right now? */
    public void addStop(int floor) {
        if (floor == currentFloor) {
            openDoor();
            return;
        }
        if (floor > currentFloor) {
            upStops.add(floor);
        } else {
            downStops.add(floor);
        }
    }

    /** LOOK: only reverse once the current direction's set is exhausted. */
    public int getNextStop() {
        if (currentDirection == Direction.UPWARD) {
            if (!upStops.isEmpty()) return upStops.first();
            if (!downStops.isEmpty()) {
                currentDirection = Direction.DOWNWARD;
                return downStops.first();
            }
        } else {
            if (!downStops.isEmpty()) return downStops.first();
            if (!upStops.isEmpty()) {
                currentDirection = Direction.UPWARD;
                return upStops.first();
            }
        }
        return -1;
    }

    public void moveOneStep() {
        int destination = getNextStop();

        if (destination == -1) {
            elevatorState = ElevatorState.IDLE;
            return;
        }

        elevatorState = ElevatorState.MOVING;

        if (destination > currentFloor) {
            currentFloor++;
        } else if (destination < currentFloor) {
            currentFloor--;
        }

        if (destination == currentFloor) {
            openDoor();
            if (currentDirection == Direction.UPWARD) {
                upStops.remove(currentFloor);
            } else {
                downStops.remove(currentFloor);
            }
            closeDoor();
            // Simplification: door open/close happens synchronously here.
            // Production systems would model a timed OPEN duration via a
            // scheduler to allow real boarding time.
        }
    }

    public void openDoor() { doorState = DoorState.OPEN; }
    public void closeDoor() { doorState = DoorState.CLOSE; }

    public boolean hasCapacity() { return currentLoad < capacity; }

    public void boardPassenger() {
        if (!hasCapacity()) {
            throw new IllegalStateException("Elevator " + elevatorId + " is at capacity");
        }
        currentLoad++;
    }

    public void disembarkPassenger() {
        if (currentLoad > 0) currentLoad--;
    }

    public int getElevatorId() { return elevatorId; }
    public int getCapacity() { return capacity; }
    public int getCurrentFloor() { return currentFloor; }
    public ElevatorState getElevatorState() { return elevatorState; }
    public DoorState getDoorState() { return doorState; }
    public Direction getCurrentDirection() { return currentDirection; }
}
```

### ElevatorManager

```java
package com.example.interviewQuestions.LLD.Code.Elevator;

import com.example.interviewQuestions.LLD.Code.Elevator.State.Direction;
import com.example.interviewQuestions.LLD.Code.Elevator.State.ElevatorState;
import com.example.interviewQuestions.LLD.Code.Elevator.State.RequestState;

import java.util.ArrayList;
import java.util.List;

public class ElevatorManager {
    private final List<Elevator> elevators;

    public ElevatorManager() {
        this.elevators = new ArrayList<>();
    }

    public void addElevator(Elevator elevator) { elevators.add(elevator); }
    public List<Elevator> getElevators() { return elevators; }

    /**
     * Cost function accounts for intermediate-stop overhead, not just
     * raw floor distance (door dwell time at each stop is real time).
     */
    public Elevator assignRequest(ExternalRequest request) {
        Elevator best = null;
        int bestCost = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            if (!e.hasCapacity()) continue;

            int cost = computeCost(e, request);
            if (cost < bestCost) {
                bestCost = cost;
                best = e;
            }
        }

        if (best != null) {
            best.addStop(request.getFloor());
            request.setRequestState(RequestState.ASSIGNED);
        }
        return best;
    }

    private int computeCost(Elevator e, ExternalRequest request) {
        int distance = Math.abs(e.getCurrentFloor() - request.getFloor());

        if (e.getElevatorState() == ElevatorState.IDLE) {
            return distance;
        }

        boolean sameDirection = e.getCurrentDirection() == request.getDirection();
        boolean isAhead = request.getDirection() == Direction.UPWARD
                ? request.getFloor() >= e.getCurrentFloor()
                : request.getFloor() <= e.getCurrentFloor();

        if (sameDirection && isAhead) {
            return distance; // on the way — best case
        }

        return distance + 1000; // must finish sweep, reverse, come back
    }
}
```

### Main (client / driver)

```java
package com.example.interviewQuestions.LLD.Code.Elevator;

import com.example.interviewQuestions.LLD.Code.Elevator.State.Direction;
import com.example.interviewQuestions.LLD.Code.Elevator.State.ElevatorState;

public class Main {
    public static void main(String[] args) {
        ElevatorManager manager = new ElevatorManager();

        manager.addElevator(new Elevator(1, 2, 5));
        manager.addElevator(new Elevator(2, 10, 5));
        manager.addElevator(new Elevator(3, 6, 5));
        manager.addElevator(new Elevator(4, 15, 5));

        ExternalRequest req1 = new ExternalRequest(6, Direction.UPWARD, System.currentTimeMillis());
        Elevator assigned = manager.assignRequest(req1);
        System.out.println("Request for floor 6 (UP) assigned to Elevator "
                + (assigned != null ? assigned.getElevatorId() : "NONE"));

        ExternalRequest req2 = new ExternalRequest(9, Direction.DOWNWARD, System.currentTimeMillis());
        Elevator assigned2 = manager.assignRequest(req2);
        System.out.println("Request for floor 9 (DOWN) assigned to Elevator "
                + (assigned2 != null ? assigned2.getElevatorId() : "NONE"));

        // Internal request — goes straight to the elevator, no manager involved.
        if (assigned != null) {
            InternalRequest internalReq = new InternalRequest(12, System.currentTimeMillis());
            assigned.addStop(internalReq.getFloor());
            System.out.println("Passenger inside Elevator " + assigned.getElevatorId()
                    + " requested floor " + internalReq.getFloor());
        }

        System.out.println("\n--- Simulating movement ---");
        for (Elevator e : manager.getElevators()) {
            simulate(e);
        }
    }

    private static void simulate(Elevator elevator) {
        int safetyLimit = 50;
        while (safetyLimit-- > 0) {
            int before = elevator.getCurrentFloor();
            elevator.moveOneStep();

            if (elevator.getElevatorState() == ElevatorState.IDLE) {
                System.out.println("Elevator " + elevator.getElevatorId()
                        + " is now IDLE at floor " + elevator.getCurrentFloor());
                break;
            }

            if (elevator.getCurrentFloor() != before) {
                System.out.println("Elevator " + elevator.getElevatorId()
                        + " moved to floor " + elevator.getCurrentFloor()
                        + " [" + elevator.getDoorState() + "]");
            }
        }
    }
}
```

---

## 7. Areas to Keep Drilling

1. **Cost functions beyond raw distance** — this was the single biggest gap surfaced (Q3 in the
   deep-dive round). Always ask "what real-world time cost am I ignoring?" before finalizing a
   scoring function.
2. **Separating concurrency issues from modeling issues** — duplicate-request handling is a
   data-modeling question, not inherently a thread-safety one; keep the two distinct in your head.
3. **Verbal delivery** — state the conclusion first, then justify, rather than reasoning out loud
   in real time. The reasoning quality was consistently strong; the delivery needs tightening for
   a live interview setting.