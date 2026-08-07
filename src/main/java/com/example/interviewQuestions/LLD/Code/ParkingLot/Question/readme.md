# Parking Lot — LLD Mock Interview: Full Notes & Final Code

A complete record of a mock Low-Level Design interview: requirements gathering, design evolution, mistakes made during coding, corrections, and the final working solution.

---

## 1. Problem Statement

Design a Parking Lot Management System supporting:
- Multiple floors
- Multiple vehicle types (motorcycle, car, truck/bus)
- Multiple spot types (compact, large, motorcycle) — vehicle can only park in a compatible spot
- Multiple entry/exit gates issuing/processing tickets
- Fee calculation based on duration parked
- Finding the nearest available spot for a vehicle

---

## 2. Requirements Clarified (Candidate-Driven)

Good interview practice: clarify scope **before** designing. Questions asked and answers given:

| Question | Answer |
|---|---|
| Fee different per vehicle type? | Yes — different hourly rates per type. Keep pricing logic swappable, not hardcoded. |
| Multiple entry/exit points? | Yes — multiple gates, each independently issuing tickets / processing exits. |
| Vehicle categorization? | Keep simple: Motorcycle, Car, Truck/Bus. |
| How many floors? | Configurable — don't hardcode a count. |
| Flat price or duration-based? | Duration-based (hourly), but extensible to other pricing models later. |
| Payment modes? | Multiple — cash, card, UPI. Design for easy addition of new methods, no gateway integration needed. |

---

## 3. High-Level Design Evolution

### 3.1 Core Entities (first pass)
`Vehicle`, `Ticket`, `ParkingLotManager`, `ParkingFloor`, `ParkingSpot`, plus enums `VehicleType`, `SpotType`, `PaymentType`, `EntryGate`/`ExitGate`.

### 3.2 Design Gaps Found During Review

**Gap 1 — `ParkingLotManager` had a single `Vehicle vehicle` / `Ticket ticket` field.**
Problem: a manager coordinating the *entire* lot can't hold state for only one vehicle/ticket at a time — what happens with 50 simultaneously parked cars?
**Fix:** `Map<String, Ticket> activeTickets` to track all active sessions.

**Gap 2 — No reference from `ParkingLotManager` to `List<ParkingFloor>`.**
Problem: `parkVehicle()` needs to search floors for a spot, but the manager had no way to reach them.
**Fix:** added `List<ParkingFloor> parkingFloors`.

**Gap 3 — `ParkingSpot` had no reference to which vehicle occupies it.**
Problem: on exit, how do you find which spot to free without scanning every spot on every floor?
**Fix (first pass):** added `Vehicle vehicle` to `ParkingSpot` — but this is a *reverse* lookup, still requiring an O(all spots) scan from the exit side.
**Real fix:** `Ticket` should hold a reference to `ParkingSpot` (assigned at entry). Exit then becomes O(1): `ticket → spot`, mark free, done.

**Ticket ID as the map key** — chosen deliberately, because the physical ticket is what the driver hands over at the exit gate; the system should never require them to know an internal `vehicleId`.

---

## 4. Design Pattern Decisions

### 4.1 Fee Calculation — Strategy Pattern (not Decorator)
Initially considered Decorator, self-corrected to **Strategy** — the right axis, since fee calculation is about *swapping an algorithm* (how to compute a fee), not *layering behavior* onto an object.

**Key decision: one strategy per pricing MODEL, not per vehicle type.**
- ✅ `HourlyStrategy`, `FlatRateStrategy`, `FirstHourFreeStrategy`
- ❌ `BikeFeeStrategy`, `CarFeeStrategy`, `TruckFeeStrategy` (would explode combinatorially as vehicle types grow)

**Rate lookup:** `Map<SpotType, Double> hourlyRates` — keyed by `SpotType`, not `VehicleType`.
Reasoning: there are far fewer spot types than potential future vehicle types, and vehicle type already maps to spot type at assignment time, so reusing that axis avoids introducing a second, redundant one.

### 4.2 Factory Pattern — scoped correctly after a false start
Initial instinct: `VehicleFactory` — **incorrect**. `Vehicle` is just `(id, type)`, no branching construction logic, so Factory adds no value there.

**Rule established:** Factory earns its place only when:
1. Creation branches into genuinely different subclasses based on a type, **and**
2. Each subclass has different construction/initialization logic.

**Correct application: `PaymentFactory`** — returns `CashPayment`, `CardPayment`, or `UPIPayment` based on `PaymentType`. This is the strongest real candidate in this design.

**Why Factory + program-to-an-interface are paired:** the caller (`ParkingLotManager`) only ever depends on the `Payment` interface (`pay(amount)`) — it never needs to know the concrete class. This means adding a new payment method later (e.g., Wallet) requires **one new class + one line in the factory**, with zero changes to `ParkingLotManager`. That's Open/Closed Principle in practice.

---

## 5. Concurrency Deep Dive

### 5.1 The Problem
Two vehicles arrive at two different entry gates simultaneously. Both call `findSpot()`, both see the same free spot as available, both try to claim it.

### 5.2 Locking Granularity
- ❌ Method-level lock on `findSpot()` or lock the whole floor — kills parallelism; one gate parking a car shouldn't block another gate.
- ✅ **Lock at the `ParkingSpot` level** — different threads can claim different spots in parallel.

### 5.3 Boolean flag isn't enough
A plain `boolean isOccupied` isn't thread-safe. The check (`isOccupied == false`) and the set (`isOccupied = true`) must happen as **one atomic unit**, or two threads can both pass the check before either writes the flag.

### 5.4 Lock-free vs. blocking — corrected terminology
- **`ReentrantLock`** — a **blocking** mechanism. A thread that can't acquire it is suspended by the OS and woken later (real context-switch cost).
- **`AtomicBoolean.compareAndSet()`** — **non-blocking**, maps to a CPU-level CAS instruction. Threads that fail just retry; no OS involvement.

*(Correction made during the session: it is not that CAS "locks internally" — it's the opposite. `ReentrantLock` has real blocking/locking semantics; CAS avoids locking entirely. This is the correct vocabulary to use in an actual interview.)*

**Decision:** `AtomicBoolean` with CAS — appropriate for a simple flag flip that's not a high-contention hot path.

### 5.5 The multi-field atomicity trap (the hardest concept of the session)
Assigning a spot isn't just flipping a boolean — it's **three related writes**: `isOccupied`, `spot.vehicle`, and `ticket.spot`. A bare `AtomicBoolean` only guarantees atomicity for *one* field.

**Failure scenario:**
1. Thread A: `compareAndSet(false, true)` succeeds on Spot #5.
2. Thread A pauses *before* setting `spot.vehicle`.
3. Thread B reads Spot #5, sees `occupied = true` but `vehicle = null`.
4. Correctness bug — an outside observer can witness a genuinely inconsistent intermediate state (not just a timing/visibility issue that resolves itself).

**Fix options:**
- `synchronized(spot) { ... }` wrapping the check + all three writes (simplest, correct, negligible cost for a rare/fast operation).
- Encapsulate via a single `assign(vehicle, ticket)` method on `ParkingSpot` that never exposes partially-updated state to external callers.
- Bundle `isOccupied + vehicle + ticket` into one immutable object, swapped via a single `AtomicReference<SpotAssignment>`.

**Takeaway:** the moment an operation becomes "N related writes that must be consistent together," you've left "atomic primitive" territory and entered "guard the whole operation" territory.

---

## 6. SRP / Responsibility Refactor

### 6.1 The Violation
Original `ParkingLotManager` bundled parking, unparking, *and* fee calculation — multiple reasons to change (pricing policy changes vs. parking policy changes).

### 6.2 Final Class Breakdown

| Class | Owns |
|---|---|
| `ParkingSpotManager` | Find / reserve / release spots across floors |
| `TicketManager` | Create tickets, own `activeTickets` map, close tickets |
| `FeeCalculator` | Look up duration + rate, delegate to `FeeStrategy` |
| `PaymentManager` | Use `PaymentFactory`, call `.pay()` |
| `ParkingLotManager` | Pure coordinator — zero business logic, zero state of its own |

### 6.3 Entry Flow Trace (verified correct)
```
EntryGate → ParkingLotManager.parkVehicle(vehicle)
          → ParkingSpotManager.reserveSpot(vehicle)   [finds + atomically claims spot]
          → TicketManager.createTicket(vehicle, spot) [builds ticket, stores in activeTickets]
          → returns Ticket → EntryGate → driver
```

---

## 7. Rules for Assigning Method Responsibility to Classes

A recurring struggle during the session: identifying entities/patterns is easier than deciding *which class a method belongs on*. Rules established:

1. **"Who owns the data, owns the method."** If a method mainly reads/writes a class's own fields, it belongs there. Red flag: a method that reaches into another object's fields via getters to do its job — that logic probably belongs on *that* object instead. (E.g., `getDurationInHours()` should live on `Ticket`, using its own `entryTime`/`exitTime` — not take them as parameters.)

2. **Entity vs. Manager — "does it know about the collection?"**
    - Entity methods operate on **one instance's own state** (`ticket.getDuration()`).
    - Manager methods operate **across a collection** — searching, creating, indexing, enforcing uniqueness (`ticketManager.createTicket()`).

3. **The "reason to change" test (SRP, concretely applied).** For each method, ask: *what policy change would force me to edit this?* Group methods that share an answer into the same class; split ones that don't.

4. **Coordinators should not "do work."** If a coordinator class (`ParkingLotManager`) contains a loop, a calculation, or a business-rule conditional, that logic escaped its proper home. A good coordinator method is a straight list of `someOtherClass.doSomething(...)` calls.

5. **"Who'd be surprised if this method were missing?"** — a fast gut-check for entity-level methods when stuck.

**Practical exercise recommended:** for any new class, first pass — write the smallest useful methods computable from its own fields (entity methods). Second pass — separately list operations needing search/create/enforce-across-many-instances (manager methods). Doing these as two distinct passes prevents muddling.

---

## 8. Mistakes Made While Coding (Chronological, With Root Causes)

This section is the most important part to review before the next interview — these are **process** mistakes, not knowledge gaps.

### 8.1 Interface mismatches between caller and callee
Repeatedly, a class's method signature or name was changed, but the class(es) calling it were not updated in the same pass:
- `parkingSpot.assignVehicle(vehicle)` called, but method was actually named `assignSpot()` (later corrected to `assignVehicle()` — but the caller lagged for several rounds).
- `ticketManager.createTickets(vehicle, parkingSpot)` called expecting a `Ticket` return, while the method was `void createTickets(String ticketId, Ticket ticket)` — completely different contract.

**Root cause:** editing one file in isolation without tracing who calls it.
**Fix going forward:** whenever a method signature changes, immediately open every caller and update them in the same sitting.

### 8.2 Design decisions agreed upon verbally but never implemented in code
The multi-field atomicity fix for `ParkingSpot.assignSpot()` was discussed and agreed on at length — but the method was left as an empty stub for several rounds afterward.

**Root cause:** translating a spoken/written design decision into an actual method body is a separate step from *agreeing* on the decision, and it was being skipped.
**Fix going forward:** the moment a design decision is made, write a one-line TODO directly in the method stub so it isn't lost.

### 8.3 The `AtomicBoolean.equals(false)` bug
```java
if(parkingSpot.getIsOccupied().equals(false)){ ... }
```
`AtomicBoolean` does not override `.equals()` — this compares object identity against an autoboxed `Boolean`, which is **always false**. Effect: `findAvailableSpot()` always returned `null`, silently, with no exception — the worst kind of bug because nothing crashes, it just never works.
**Fix:** `!parkingSpot.getIsOccupied().get()`.
**Lesson:** always use `.get()` to read an `Atomic*` wrapper's value, never `.equals()`.

### 8.4 Redundant parameters on methods that already own the data
`Ticket.getDuration(LocalTime entryTime, LocalTime exitTime)` took parameters despite `Ticket` already storing both fields — the caller was fetching them via getters just to hand them straight back in. Symptomatic of not applying Rule #1 from Section 7 consistently. Flagged and left unfixed across **three separate rounds** before finally being corrected — a good illustration of how easy it is for a flagged issue to get lost when many other classes are being edited at the same time.

### 8.5 Unguarded map lookups causing NPEs on auto-unboxing
```java
double vehicleRate = hourlyRates.get(spotType); // NPE if key missing
```
Any `Map<K, Double>.get()` returning `null` will NPE the moment it's auto-unboxed into a primitive `double`. Same root issue appeared in `getRequiredSpotType()`'s missing `default` case, silently returning `null` for an unhandled enum value, causing an NPE far downstream from the actual cause.
**Fix pattern:** always check for `null` explicitly on map lookups feeding into primitives, and always add a `default` (throwing, not silently returning `null`) to any `switch` over an enum used for business logic.

### 8.6 Never wiring the "find" and "claim" steps together
`ParkingSpotManager.reserveSpot()` called `floor.findAvailableSpot()` and returned the result directly — **never calling `spot.assignVehicle(vehicle)`**. All the CAS logic written into `ParkingSpot` was correct but entirely unused dead code for several rounds, meaning the original race condition being solved for was still fully present in the assembled system.
**Fix:** `find` and `claim` must be chained, with a fallback to keep searching if the claim (CAS) fails:
```java
if (spot != null && spot.assignVehicle(vehicle)) { return spot; }
```

### 8.7 No data to operate on
`ParkingFloor` had no `addSpot()` method, and `MainClient` never constructed or registered any `ParkingFloor`/`ParkingSpot` objects. The system "ran" without crashing but never actually exercised the real logic — `reserveSpot()`'s search loop had nothing to search.
**Lesson:** a program that runs without throwing is not the same as a program that's been verified to do the right thing — need to assert/print the *actual assigned values*, not just that a ticket number was printed.

### 8.8 `LocalTime` vs `LocalDateTime`
Using `LocalTime` for `entryTime`/`exitTime` has no date component — if a vehicle enters at 11 PM and exits at 1 AM, `Duration.between()` computes a **negative** duration, since it can't detect the day rolled over. Should use `LocalDateTime`.

### 8.9 Enum inconsistency
Original enum was `VehicleType { BIKE, CAR, TRUCK }`, but later code used `VehicleType.BUS` in multiple places (`MainClient`, `getRequiredSpotType()`). Never reconciled — a silent source of missing-case bugs.

### 8.10 Pattern behind all the mistakes
Two consistent failure modes across the whole session:
1. **Generating a correct piece in isolation ≠ integrating it correctly** — a class was fixed, but its collaborators weren't updated to match.
2. **No compile/dry-run discipline while writing** — several bugs (mismatched signatures, non-existent methods) were things a compiler would catch instantly, but accumulated silently across many classes before being caught in review.

**Concrete practice going forward:** after finishing each class, don't move to the next one — go back and verify every class that calls into it still lines up. Treat "wire it up correctly" as part of writing the class, not a separate cleanup pass done later.

---

## 9. Mock Interview Rating

**Overall: 7/10 — Strong Pass for 3 YOE**, borderline Strong Pass/Exceeds on design thinking, held back by concurrency depth.

| Area | Score | Notes |
|---|---|---|
| Requirements gathering | 9/10 | Strong, unprompted clarifying questions before designing |
| Core entity modeling | 7/10 | Solid first pass; needed prompting to fix Ticket↔Spot linkage and O(n) exit lookup |
| Design patterns (Strategy/Factory) | 8/10 | Correct final choices with real justification; self-corrected well under pressure |
| SOLID / responsibility design | 8/10 | Clean SRP breakdown, correctly traced data flow, no class overlap |
| Concurrency | 5/10 | Right instincts (per-spot locking, lock-free CAS), but couldn't self-derive multi-field atomicity or articulate CAS-vs-lock correctly unprompted |
| Communication/reasoning | 8/10 | Explained tradeoffs, not just pattern names — this matters a lot in real interviews |

**Verdict:** Would likely clear an LLD round at most mid-to-large product companies at SDE-2 / 3 YOE level. At bars that push harder on concurrency (fintech, infra-heavy companies, some FAANG rounds), the multi-field atomicity gap is worth drilling separately before the real interview.

---

## 10. Final Corrected Code

### Enums

```java
public enum VehicleType { BIKE, CAR, TRUCK }
```
```java
public enum SpotType { SMALL, COMPACT, LARGE }
```
```java
public enum PaymentType { CASH, CARD, UPI }
```

### Entities

**`Vehicle.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Entities;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.VehicleType;

public class Vehicle {
    private final String vehicleId;
    private final VehicleType vehicleType;

    public Vehicle(String vehicleId, VehicleType vehicleType) {
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }
}
```

**`ParkingSpot.java`** — CAS-based claim, single-field atomicity is sufficient here because `vehicle` is only ever read *after* a successful `compareAndSet`, and only ever set *by* the thread that won the CAS (see note below).
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Entities;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;

import java.util.concurrent.atomic.AtomicBoolean;

public class ParkingSpot {
    private final String spotId;
    private final SpotType spotType;
    private volatile Vehicle vehicle;
    private final AtomicBoolean isOccupied;

    public ParkingSpot(String spotId, SpotType spotType) {
        this.spotId = spotId;
        this.spotType = spotType;
        this.isOccupied = new AtomicBoolean(false);
    }

    /**
     * Attempts to atomically claim this spot for the given vehicle.
     * Returns true if this call won the race; false if the spot was already taken.
     * NOTE: only the winning thread ever writes `vehicle`, so there is no
     * multi-field race here — but if you extend this to also stamp a Ticket
     * reference on the spot, wrap the whole block in synchronized(this) instead.
     */
    public boolean assignVehicle(Vehicle vehicle) {
        if (isOccupied.compareAndSet(false, true)) {
            this.vehicle = vehicle;
            return true;
        }
        return false;
    }

    public synchronized void release() {
        this.vehicle = null;
        isOccupied.set(false);
    }

    public String getSpotId() {
        return spotId;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public AtomicBoolean getIsOccupied() {
        return isOccupied;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}
```

**`ParkingFloor.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Entities;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkingFloor {
    private final int floorNumber;
    private final Map<SpotType, List<ParkingSpot>> spots;

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.spots = new HashMap<>();
    }

    public void addSpot(ParkingSpot spot) {
        spots.computeIfAbsent(spot.getSpotType(), k -> new ArrayList<>()).add(spot);
    }

    public ParkingSpot findAvailableSpot(SpotType spotType) {
        List<ParkingSpot> parkingSpots = spots.get(spotType);
        if (parkingSpots == null) {
            return null;
        }
        for (ParkingSpot parkingSpot : parkingSpots) {
            if (!parkingSpot.getIsOccupied().get()) {
                return parkingSpot;
            }
        }
        return null;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Map<SpotType, List<ParkingSpot>> getSpots() {
        return spots;
    }
}
```

**`Ticket.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Entities;

import java.time.Duration;
import java.time.LocalDateTime;

public class Ticket {
    private final String ticketNumber;
    private final ParkingSpot parkingSpot;
    private final Vehicle vehicle;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;

    public Ticket(String ticketNumber, LocalDateTime entryTime, ParkingSpot parkingSpot, Vehicle vehicle) {
        this.ticketNumber = ticketNumber;
        this.entryTime = entryTime;
        this.parkingSpot = parkingSpot;
        this.vehicle = vehicle;
    }

    public void closeTicket(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public double getDurationInHours() {
        if (exitTime == null) {
            throw new IllegalStateException("Cannot calculate duration before exitTime is set");
        }
        return Duration.between(entryTime, exitTime).toMinutes() / 60.0;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getParkingSpot() {
        return parkingSpot;
    }
}
```

### Strategy (Fee Calculation)

**`FeeStrategy.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;

public interface FeeStrategy {
    double calculateFee(Ticket ticket);
}
```

**`HourlyPricingFeeStrategy.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;

import java.util.HashMap;
import java.util.Map;

public class HourlyPricingFeeStrategy implements FeeStrategy {

    private final Map<SpotType, Double> hourlyRates;

    public HourlyPricingFeeStrategy() {
        hourlyRates = new HashMap<>();
        hourlyRates.put(SpotType.SMALL, 100.0);
        hourlyRates.put(SpotType.COMPACT, 300.0);
        hourlyRates.put(SpotType.LARGE, 500.0);
    }

    @Override
    public double calculateFee(Ticket ticket) {
        SpotType spotType = ticket.getParkingSpot().getSpotType();

        Double rate = hourlyRates.get(spotType);
        if (rate == null) {
            throw new IllegalStateException("No hourly rate configured for spot type: " + spotType);
        }

        double duration = ticket.getDurationInHours();
        return duration * rate;
    }
}
```

**`FlatPriceStrategy.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;

public class FlatPriceStrategy implements FeeStrategy {
    @Override
    public double calculateFee(Ticket ticket) {
        return 400.0;
    }
}
```

**`FeeCalculator.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy.FeeStrategy;

public class FeeCalculator {
    private final FeeStrategy feeStrategy;

    public FeeCalculator(FeeStrategy feeStrategy) {
        this.feeStrategy = feeStrategy;
    }

    public double calculateFee(Ticket ticket) {
        return feeStrategy.calculateFee(ticket);
    }
}
```

### Payment (Factory)

**`Payment.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Payment;

public interface Payment {
    void pay(double amount);
}
```

**`CashPayment.java` / `CardPayment.java` / `UPIPayment.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Payment;

public class CashPayment implements Payment {
    @Override
    public void pay(double amount) {
        System.out.println("Paid ₹" + amount + " via Cash");
    }
}
```
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Payment;

public class CardPayment implements Payment {
    @Override
    public void pay(double amount) {
        System.out.println("Paid ₹" + amount + " via Card");
    }
}
```
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Payment;

public class UPIPayment implements Payment {
    @Override
    public void pay(double amount) {
        System.out.println("Paid ₹" + amount + " via UPI");
    }
}
```

**`PaymentFactory.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Payment;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.PaymentType;

public class PaymentFactory {
    public static Payment createPayment(PaymentType type) {
        switch (type) {
            case CASH: return new CashPayment();
            case CARD: return new CardPayment();
            case UPI: return new UPIPayment();
            default: throw new IllegalArgumentException("Unsupported payment type: " + type);
        }
    }
}
```

**`PaymentManager.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.PaymentType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Payment.Payment;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Payment.PaymentFactory;

public class PaymentManager {
    public void processPayment(PaymentType type, double amount) {
        Payment payment = PaymentFactory.createPayment(type);
        payment.pay(amount);
    }
}
```

### Managers

**`ParkingSpotManager.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingFloor;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;

import java.util.ArrayList;
import java.util.List;

public class ParkingSpotManager {
    private final List<ParkingFloor> floors;

    public ParkingSpotManager() {
        this.floors = new ArrayList<>();
    }

    public void addParkingFloor(ParkingFloor parkingFloor) {
        floors.add(parkingFloor);
    }

    public ParkingSpot reserveSpot(Vehicle vehicle) {
        SpotType spotType = getRequiredSpotType(vehicle);
        if (spotType == null) {
            throw new IllegalArgumentException("Unsupported vehicle type: " + vehicle.getVehicleType());
        }

        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.findAvailableSpot(spotType);
            if (spot != null && spot.assignVehicle(vehicle)) {
                return spot; // found AND successfully claimed via CAS
            }
            // else: no spot here, or lost the race to another thread — keep looking
        }
        return null; // no spot available anywhere
    }

    public void releaseSpot(ParkingSpot spot) {
        spot.release();
    }

    private SpotType getRequiredSpotType(Vehicle vehicle) {
        switch (vehicle.getVehicleType()) {
            case BIKE: return SpotType.SMALL;
            case CAR: return SpotType.COMPACT;
            case TRUCK: return SpotType.LARGE;
            default: return null;
        }
    }
}
```

**`TicketManager.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TicketManager {
    private final Map<String, Ticket> activeTickets;

    public TicketManager() {
        this.activeTickets = new HashMap<>();
    }

    public Ticket createTicket(Vehicle vehicle, ParkingSpot parkingSpot) {
        String ticketId = UUID.randomUUID().toString();
        Ticket ticket = new Ticket(ticketId, LocalDateTime.now(), parkingSpot, vehicle);
        activeTickets.put(ticketId, ticket);
        return ticket;
    }

    public Ticket findTicket(String ticketId) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("No active ticket found for id: " + ticketId);
        }
        return ticket;
    }

    public Ticket closeTicket(String ticketId) {
        Ticket ticket = findTicket(ticketId);
        ticket.closeTicket(LocalDateTime.now());
        activeTickets.remove(ticketId);
        return ticket;
    }
}
```

### Coordinator

**`ParkingLotManager.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.PaymentType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.FeeCalculator;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.ParkingSpotManager;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.PaymentManager;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.TicketManager;

public class ParkingLotManager {
    private final TicketManager ticketManager;
    private final ParkingSpotManager parkingSpotManager;
    private final FeeCalculator feeCalculator;
    private final PaymentManager paymentManager;

    public ParkingLotManager(TicketManager ticketManager,
                              ParkingSpotManager parkingSpotManager,
                              FeeCalculator feeCalculator,
                              PaymentManager paymentManager) {
        this.ticketManager = ticketManager;
        this.parkingSpotManager = parkingSpotManager;
        this.feeCalculator = feeCalculator;
        this.paymentManager = paymentManager;
    }

    public Ticket parkVehicle(Vehicle vehicle) {
        ParkingSpot parkingSpot = parkingSpotManager.reserveSpot(vehicle);
        if (parkingSpot == null) {
            throw new IllegalStateException("No available spot for vehicle: " + vehicle.getVehicleId());
        }
        return ticketManager.createTicket(vehicle, parkingSpot);
    }

    public double unparkVehicle(String ticketId, PaymentType paymentType) {
        Ticket ticket = ticketManager.closeTicket(ticketId);
        double fee = feeCalculator.calculateFee(ticket);
        paymentManager.processPayment(paymentType, fee);
        parkingSpotManager.releaseSpot(ticket.getParkingSpot());
        return fee;
    }
}
```

### Driver

**`MainClient.java`**
```java
package com.example.interviewQuestions.LLD.Code.ParkingLot;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingFloor;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.PaymentType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.VehicleType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.FeeCalculator;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.ParkingSpotManager;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.PaymentManager;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy.FeeStrategy;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy.HourlyPricingFeeStrategy;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.TicketManager;

public class MainClient {
    public static void main(String[] args) throws InterruptedException {
        TicketManager ticketManager = new TicketManager();
        ParkingSpotManager parkingSpotManager = new ParkingSpotManager();
        FeeStrategy feeStrategy = new HourlyPricingFeeStrategy();
        FeeCalculator feeCalculator = new FeeCalculator(feeStrategy);
        PaymentManager paymentManager = new PaymentManager();

        ParkingLotManager parkingLotManager =
                new ParkingLotManager(ticketManager, parkingSpotManager, feeCalculator, paymentManager);

        // Set up a floor with one spot of each type
        ParkingFloor floor1 = new ParkingFloor(1);
        floor1.addSpot(new ParkingSpot("S1", SpotType.SMALL));
        floor1.addSpot(new ParkingSpot("C1", SpotType.COMPACT));
        floor1.addSpot(new ParkingSpot("L1", SpotType.LARGE));
        parkingSpotManager.addParkingFloor(floor1);

        Vehicle bike = new Vehicle("UP653522", VehicleType.BIKE);
        Vehicle car = new Vehicle("MH235748", VehicleType.CAR);

        // Park
        Ticket bikeTicket = parkingLotManager.parkVehicle(bike);
        System.out.println("Bike Ticket: " + bikeTicket.getTicketNumber()
                + " | Spot: " + bikeTicket.getParkingSpot().getSpotId());

        Ticket carTicket = parkingLotManager.parkVehicle(car);
        System.out.println("Car Ticket: " + carTicket.getTicketNumber()
                + " | Spot: " + carTicket.getParkingSpot().getSpotId());

        // Simulate some parked duration
        Thread.sleep(1000);

        // Unpark + pay
        double bikeFee = parkingLotManager.unparkVehicle(bikeTicket.getTicketNumber(), PaymentType.UPI);
        System.out.println("Bike fee charged: ₹" + bikeFee);

        double carFee = parkingLotManager.unparkVehicle(carTicket.getTicketNumber(), PaymentType.CARD);
        System.out.println("Car fee charged: ₹" + carFee);
    }
}
```

---

## 11. What Changed From Your Last Version (Quick Diff Summary)

- `ParkingFloor.findAvailableSpot()`: `.equals(false)` → `!....get()`
- `ParkingFloor`: added `addSpot()`
- `ParkingSpotManager.reserveSpot()`: now calls `spot.assignVehicle(vehicle)` and only returns on a successful claim; continues searching on CAS failure
- `ParkingSpotManager.getRequiredSpotType()`: added `default` case, fixed `BUS` → `TRUCK` to match the enum
- `Ticket`: switched `LocalTime` → `LocalDateTime`; `getDuration(entryTime, exitTime)` → zero-arg `getDurationInHours()`; added `closeTicket(exitTime)`
- `TicketManager`: added `closeTicket()`, renamed `createTickets`/`findTickets` → `createTicket`/`findTicket`
- New: `PaymentManager`, `PaymentFactory`, `Payment` interface + 3 implementations
- `ParkingLotManager`: added `unparkVehicle()`, wired in `PaymentManager`
- `MainClient`: now actually builds and registers floors/spots, and exercises the full park → unpark → pay flow end to end