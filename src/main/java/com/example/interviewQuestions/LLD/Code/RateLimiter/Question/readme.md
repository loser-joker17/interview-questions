# Rate Limiter — LLD Mock Interview Notes

A full record of the mock interview: requirements gathering, algorithm mechanics,
design decisions, mistakes made, corrections applied, and the final working code.

---

## 1. Requirements Gathering

### Clarifying questions asked (good practice, asked before any design)

| Question | Answer / Resolution |
|---|---|
| Rate-limit by user, IP, or flexible? | **Flexible/generic** — design around a generic "key" concept (user ID, IP, API key), not hardcoded to one identifier type. |
| Is there a specific criteria for limiting requests? | **N requests per configurable time window** (e.g., 100 req/min). Multiple algorithms can implement this rule differently. |
| Should the system inform the caller when the limit is hit? | Yes — **reject** the request (conceptually HTTP 429 Too Many Requests) rather than silently drop or queue indefinitely. |
| Different limits for different user tiers (e.g., subscriptions)? | Yes — **Free vs Premium**, configurable per tier, not hardcoded. |

### Functional Requirements (FRs)

1. Rate-limit requests based on a **generic key** (user ID, IP, API key) — not hardcoded to one identifier type.
2. Allow **N requests within a configurable time window**.
3. Reject requests that exceed the limit (equivalent to HTTP 429).
4. Support **different limits per client tier** (Free vs Premium), configurable, not hardcoded.
5. Support **multiple pluggable rate-limiting algorithms** (Token Bucket, Leaky Bucket, Fixed Window, Sliding Window) — adding a new algorithm shouldn't require rewriting existing code.

### Non-Functional Requirements (NFRs)

1. **Performance** — the allow/reject decision sits on the hot path of every request, so it must be **low-latency**, ideally close to O(1) overhead per request; the system shouldn't degrade under bulk/burst load.
2. **Thread-safety** — many requests for the *same* key can arrive at nearly the same instant; concurrent access to shared per-key state must not cause race conditions. This needs to be considered **per data structure choice**, not just a blanket `synchronized`.
3. **Extensibility** — swapping/adding algorithms without changing calling code. Design pattern: **Strategy Pattern**.

---

## 2. Algorithm Mechanics — Corrections Made

### Token Bucket (initial understanding was inverted — corrected)

**Wrong initial idea:** "insert requests as tokens into the bucket; bucket full = limit reached."

**Correct mechanism:**
- Bucket holds **tokens** (not requests) up to a fixed **capacity**.
- Tokens are added at a fixed **refill rate** over time, regardless of traffic.
- Each **request** must **consume one token** to proceed.
- Tokens available → **allow**, decrement by 1. Bucket empty → **reject**.
- Bucket stops filling once at capacity (excess discarded) — this is what allows short **bursts**: unused tokens accumulate while idle, then a burst can spend them all at once before falling back to the steady refill rate.

### Leaky Bucket (also corrected)

**Wrong initial idea:** mixed up with Token Bucket's "bucket size + token rate" framing.

**Correct mechanism:**
- The "bucket" is really a **queue** holding incoming **requests**.
- Requests are enqueued as they arrive.
- The queue is processed ("leaks") at a **constant, fixed output rate**, no matter how fast requests arrived.
- Queue full when a new request arrives → **reject** (overflow).

**Key behavioral difference (Token Bucket vs Leaky Bucket):** Token Bucket allows **bursts** up to capacity by spending saved-up tokens; Leaky Bucket enforces a **smooth, constant output rate** always, with no concept of saved-up capacity.

### Fixed Window — understood correctly from the start

Fixed time windows (e.g., 5 requests / 10 sec); counter resets at each window boundary.

**Known weakness (not implemented, but discussed):** the **boundary burst problem** — e.g., 5 requests at second 9 (end of window 1) plus 5 more at second 11 (start of window 2) lets 10 requests through in a 2-second span, even though each window individually respected its own 5-request cap.

### Sliding Window — introduced conceptually

Designed to fix Fixed Window's boundary-burst problem by evaluating the limit over a **rolling** window rather than resetting at fixed boundaries. Two common variants exist (log-based, storing individual timestamps; and counter-based, weighting adjacent windows) — not implemented in this session, flagged as a follow-up area.

---

## 3. Design Evolution

### Class list (final, agreed via whiteboard/diagram)

```
RateLimiter <<interface>>            +allowRequest(Request request)
   ├── TokenBucket
   ├── LeakyBucket
   ├── SlidingWindowFixed  (implements Fixed Window behavior — see naming note below)
   └── (SlidingWindowCounter — discussed, not implemented)

Request                              -userId, -ip        +getUserId(), +getIp()

UserType <<enum>>                    FREE, PREMIUM

AlgorithmType <<enum>>                TOKEN_BUCKET, LEAKY_BUCKET, FIXED_WINDOW, SLIDING_WINDOW_LOG, SLIDING_WINDOW_COUNTER

RateLimiterConfig                    capacity, refillRate, maxRequests, windowSizeSeconds
                                      (a single config object carrying all possible settings;
                                       each concrete algorithm reads only the fields it needs)

RateLimiterFactory                   +createRateLimiter(AlgorithmType, RateLimiterConfig) : RateLimiter
                                      (centralizes "which concrete class to construct" —
                                       only place in the codebase that `new`s up algorithm classes)

RateLimiterManager (has-a RateLimiter, via maps)
    - Map<UserType, RateLimiter> tierToLimiter
    - Map<String, UserType> userToTier
    +registerLimiter(UserType, RateLimiter)
    +registerUser(String userId, UserType)
    +allowRequest(Request) : boolean
```

### Key design decisions and the reasoning behind them

**Decision 1 — `Map<RequestType/UserType, RateLimiter>` instead of one `RateLimiter` per user.**

Initially considered `Map<userId, RateLimiter>` — rejected. All Free-tier users share the *same rule*; they don't need separate `RateLimiter` objects, just separate **tracked state** within one shared object. With `Map<UserType, RateLimiter>`, only ~2 `RateLimiter` instances exist total (one per tier) instead of one per user (which could be millions) — each shared instance internally manages a `Map<userId, state>` for individual tracking. Same efficiency principle as `LoggerFactory` caching in the Logging Framework round.

**Decision 2 — each algorithm owns its own internal per-user state; `RateLimiterManager` never sees it.**

`TokenBucket` internally needs `Map<userId, tokens>` + `Map<userId, lastRefillTime>`. `SlidingWindowLog` would need `Map<userId, Queue<timestamp>>`. `FixedWindow` needs count + window-start. These shapes are genuinely different — so each concrete class manages its **own** private state; `RateLimiterManager`'s job is only to route "this user → this tier's shared limiter," not to hold or understand algorithm-specific state.

**Decision 3 — Factory pattern for algorithm construction (candidate-proposed, validated).**

Reasoning given and confirmed correct: the number of algorithms is expected to grow (4 known, more likely later), and constructing the right concrete class based on a type is exactly the problem Factory solves. Centralizing `new TokenBucket(...)` / `new SlidingWindowFixed(...)` etc. inside one `RateLimiterFactory` means `RateLimiterManager` never needs to know about concrete algorithm classes — consistent with the Round 1 rule: **Manager classes should never `new` up concrete types themselves.**

Also correctly reasoned: **no Factory needed for `UserType`** — it's a small, closed, rarely-changing enum (FREE/PREMIUM), not runtime-variable like the algorithm choice. Using Factory here would be over-engineering. Good judgment distinguishing "genuine variability" from "basically fixed."

**Decision 4 — config object over long parameter lists.**

Considered passing every field (`capacity, refillRate, maxRequests, windowSize, ...`) directly into `createRateLimiter(...)`, or hardcoding constants per algorithm. Both rejected:
- Long parameter lists don't scale as more algorithms/fields are added, and most algorithms would ignore most params.
- Hardcoded constants inside each algorithm class defeats configurability (violates FR #4 — different limits per tier) and would require recompiling to change a limit.

**Resolved:** single `RateLimiterConfig` object holding all possible settings; each concrete `RateLimiter` implementation reads only the fields it actually needs from it.

**Decision 5 — where the Factory gets called (client vs. Manager constructor).**

Initially proposed calling the Factory inside `RateLimiterManager`'s constructor (hardcoding tier→algorithm setup there). **Corrected:** this repeats the exact Round 1 mistake (`AppendManager` hardcoding 3 appenders) — it freezes algorithm/config choice inside the Manager and would require editing/recompiling Manager code to change a tier's algorithm or limits, violating Open/Closed Principle.

**Final:** the Factory is called from the **client** (`main()`), which builds the concrete `RateLimiter` objects once at startup, then **registers** them into `RateLimiterManager` via `registerLimiter(UserType, RateLimiter)` — same dependency-injection pattern as `addAppender()` in the Logging Framework round. `RateLimiterManager` no longer needs a `RateLimiterFactory` field at all.

---

## 4. Bugs Found in the Code — Root Cause and Fix

### Bug #1 — `RateLimiterManager.allowRequest()` was incomplete (dangling statement, wouldn't compile)

```java
public boolean allowRequest(Request request){
    rateLimiterFactory.
}
```
**Fix:** implement the full lookup-and-delegate chain (see Section 5, Bug #4/#5 below for the complete corrected version).

### Bug #2 — Wrong map key type: `Map<Request, UserType> userToTier`

`Request` is a new object created on every single incoming call — using it as a map key means the map could never be looked up successfully across multiple requests from the same user (unless `Request` had custom `equals()`/`hashCode()` keyed on userId, which it didn't). **Fix:** key by the stable identifier — `Map<String, UserType> userToTier`, keyed by `userId`.

### Bug #3 — `tierToLimiter` was declared but never populated

No registration step existed anywhere in the class. **Fix:** added `registerLimiter(UserType tier, RateLimiter limiter)`, called from the client after building limiters via the Factory — consistent with Decision 5 above (client wires, Manager just holds/routes).

### Bug #4 — `TokenBucket`: `putIfAbsent` used instead of `put` for updating the refill timestamp

```java
if(elapsedSeconds>0){
    ...
    tokens.put(userId,newTokenCount);
    lastReffilTimeStamp.putIfAbsent(userId,currentTimeStamp);   // BUG
}
```
`putIfAbsent` only sets a value if the key is **absent** — but by this point the key already exists (set on the user's very first request), so this line silently does nothing on every subsequent request. The stored "last refill time" gets stuck at the very first request's timestamp forever, making the elapsed-time calculation on later requests measure from the *original* request instead of the *most recent* one (the bug was partially masked by the `Math.min(capacity, ...)` cap, but the underlying calculation was still wrong).

**Fix:**
```java
lastReffilTimeStamp.put(userId, currentTimeStamp);
```

### Bug #5 — Naming mismatch: `AlgorithmType.FIXED_WINDOW` mapped to a class called `SlidingWindowFixed`

The class's actual logic (single global `windowStart`, reset-on-expiry, no rolling/weighted calculation) implements **Fixed Window** behavior, but is named as if it were a Sliding Window variant. Flagged as a naming cleanup — rename to `FixedWindow` to match actual behavior and avoid confusing future readers.

### Bug #6 — `synchronized` on the whole method vs. the actual race condition (concurrency deep-dive)

Both `TokenBucket.allowRequest()` and `SlidingWindowFixed.allowRequest()` were marked `synchronized` on the entire method. Discussion walked through:

1. **Why `ConcurrentHashMap` alone isn't enough:** it guarantees individual `get()`/`put()` calls are safe, but **not** a multi-step "read, compute, write-back" sequence:
   ```java
   int currentTokens = tokens.get(userId);                 // READ
   int newTokenCount = Math.min(capacity, currentTokens + tokensToAdd);
   tokens.put(userId, newTokenCount);                       // WRITE, based on stale READ
   ```
   Two threads for the *same* userId could both read the same value before either writes back, causing a **lost update** — this is a real race condition `ConcurrentHashMap`'s per-call safety does not prevent.

2. **Why broad `synchronized` on the whole method is a real cost:** it serializes **all users** through one lock, even though only same-key read-modify-write sequences actually need protecting — directly hurting the low-latency NFR under high concurrent load from many distinct users.

3. **Resolution — CAS via `ConcurrentHashMap.compute()`:**
   ```java
   tokens.compute(userId, (id, currentTokens) ->
       Math.min(capacity, currentTokens + tokensToAdd)
   );
   ```
   `compute()` performs the read-modify-write **atomically**, internally locking only that key's bucket rather than the whole map or the whole method — narrow, per-user locking instead of broad, per-method locking. This was proposed by the candidate directly and is the more idiomatic, higher-performance solution (alternative considered: a `Map<String, Object> userLocks` + `synchronized(lock)` per user, also valid but more code).

---

## 5. Polymorphism / Dynamic Dispatch — Why `limiter.allowRequest(request)` Runs the Right Algorithm

A point of confusion worth recording explicitly:

```java
RateLimiter limiter = tierToLimiter.get(userType);
return limiter.allowRequest(request);
```

The algorithm choice was already decided **at registration time**, not at this line:

```java
RateLimiter freeLimiter = RateLimiterFactory.createRateLimiter(TOKEN_BUCKET, freeConfig);
// freeLimiter's declared type is RateLimiter, but its REAL runtime type is TokenBucket
manager.registerLimiter(UserType.FREE, freeLimiter);
```

Even though `tierToLimiter` is declared as `Map<UserType, RateLimiter>`, the actual object stored for `FREE` is a genuine `TokenBucket` instance. When `.allowRequest(request)` is called on `limiter`, Java's **dynamic dispatch** runs the method belonging to the object's *real* runtime class, not the declared interface type. This is the entire payoff of the Strategy Pattern: `RateLimiterManager` never needs an `if (algorithm == TOKEN_BUCKET)` branch anywhere — the correct behavior runs automatically based on what concrete object was registered earlier. Adding a brand-new algorithm (e.g., `LeakyBucket` for a new `ENTERPRISE` tier) requires **zero changes** to `RateLimiterManager` — only a new class plus one registration line in the client.

---

## 6. Fail-Open vs Fail-Closed — Unregistered User Edge Case

**Question raised:** what should happen if `allowRequest()` is called for a `userId` that was never registered (`userToTier.get(userId)` returns `null`)?

**Resolved approach — fail closed (reject):**
```java
if (userType == null) {
    return false;   // reject rather than allow unlimited access
}
```
Rationale: a rate limiter's purpose is to protect the system; allowing unregistered users through with no limit at all would be an easy bypass/abuse vector. Fail-closed (deny unknowns by default) is the safer default in throttling/security contexts — analogous to a firewall denying unknown traffic rather than allowing it.

**Alternative discussed, also defensible:** auto-default unregistered users to the `FREE` tier rather than rejecting outright — some real systems treat "unregistered" as "not yet configured" rather than "suspicious" (e.g., anonymous API callers bucketed into a default low tier). No single correct answer — a trade-off to be able to justify in an interview, not a fact to memorize.

---

## 7. Interview Feedback Summary (Both Rounds Combined)

**Overall verdict discussed: Lean Hire**, for a 3 YOE / SDE-2 LLD bar.

**Strengths:**
- Strong architectural instincts — converged on correct class structures largely independently across both rounds (Logger and Rate Limiter).
- Proactively proposed Factory pattern for algorithms, and correctly reasoned about **when not to** apply it (UserType) — judgment about a pattern's limits, not just reflexive application.
- Independently proposed CAS/`compute()` for the lost-update race condition — a stronger, more idiomatic answer than the simpler "per-user lock" hint being steered toward.
- Consistently asks good clarifying questions and insists on locking requirements before jumping to design.
- Self-corrects quickly once given a hint, and is honest about knowledge gaps (e.g., "not confident implementing Token Bucket") rather than bluffing through broken logic — read positively by real interviewers.

**Growth areas:**
- Design-to-code translation is the recurring gap — nearly every bug across both rounds traced back to the same root cause: a field/parameter declared but not verified as actually used in the method body (hardcoded values left in place of parameters, discarded return values, stale/incorrect map update calls like `putIfAbsent` vs `put`).
- Several correction cycles were typically needed to get from "conceptually right" to "actually correct" — fine for practice, but costly under a real interview clock.
- Some standard algorithm mechanics (Token Bucket internals) weren't known cold and had to be derived live — expected to be recall-level knowledge for commonly-asked LLD problems.

**Recommended next steps:**
1. Timed, fully unaided practice (no hints, no AI) on the same and new LLD problems — compare first-draft output against a reference afterward to identify the *actual* remaining gap.
2. Memorize the standard rate-limiting algorithm mechanics (Token Bucket, Leaky Bucket, Fixed/Sliding Window) cold before interviews.
3. Build the "trace one concrete example through every method before moving on" habit until automatic — estimated to catch the large majority of the bugs seen in both rounds.

---

## 8. Final Working Code

### `Enum/UserType.java`
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter.Enum;

public enum UserType {
    FREE,
    PREMIUM
}
```

### `Enum/AlgorithmType.java`
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter.Enum;

public enum AlgorithmType {
    TOKEN_BUCKET,
    LEAKY_BUCKET,
    FIXED_WINDOW,
    SLIDING_WINDOW_LOG,
    SLIDING_WINDOW_COUNTER
}
```

### `Request/Request.java`
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter.Request;

public class Request {
    private final String userId;
    private final String ip;

    public Request(String userId, String ip) {
        this.userId = userId;
        this.ip = ip;
    }

    public String getUserId() { return userId; }
    public String getIp() { return ip; }
}
```

### `Strategy/RateLimiter.java`
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;

public interface RateLimiter {
    boolean allowRequest(Request request);
}
```

### `Strategy/TokenBucket.java`
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucket implements RateLimiter {
    private final int capacity;
    private final int refillRate; // tokens per second
    private final Map<String, Integer> tokens;
    private final Map<String, Long> lastRefillTimestamp;

    public TokenBucket(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = new ConcurrentHashMap<>();
        this.lastRefillTimestamp = new ConcurrentHashMap<>();
    }

    @Override
    public boolean allowRequest(Request request) {
        String userId = request.getUserId();
        long currentTimestamp = System.currentTimeMillis();

        tokens.putIfAbsent(userId, capacity);
        lastRefillTimestamp.putIfAbsent(userId, currentTimestamp);

        long elapsedMillis = currentTimestamp - lastRefillTimestamp.get(userId);
        long elapsedSeconds = elapsedMillis / 1000;
        int tokensToAdd = (int) (elapsedSeconds * refillRate);

        if (elapsedSeconds > 0) {
            tokens.compute(userId, (id, currentTokens) ->
                    Math.min(capacity, currentTokens + tokensToAdd));
            lastRefillTimestamp.put(userId, currentTimestamp);
        }

        synchronized (this) {
            if (tokens.get(userId) > 0) {
                tokens.put(userId, tokens.get(userId) - 1);
                return true;
            }
            return false;
        }
    }
}
```

### `Strategy/FixedWindow.java` (renamed from `SlidingWindowFixed` per Bug #5)
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FixedWindow implements RateLimiter {
    private final int maxRequests;
    private final long windowSizeMillis;
    private volatile long windowStart;
    private final Map<String, Integer> requestCounts;

    public FixedWindow(int maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.windowStart = System.currentTimeMillis();
        this.requestCounts = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized boolean allowRequest(Request request) {
        long currentTime = System.currentTimeMillis();
        String userId = request.getUserId();

        if (currentTime - windowStart >= windowSizeMillis) {
            requestCounts.clear();
            windowStart = currentTime;
        }

        int updatedCount = requestCounts.merge(userId, 1, Integer::sum);
        return updatedCount <= maxRequests;
    }
}
```

### `Strategy/LeakyBucket.java` (stub — not fully implemented this session)
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;

public class LeakyBucket implements RateLimiter {
    @Override
    public synchronized boolean allowRequest(Request request) {
        // TODO: queue-based, constant leak-rate implementation
        return true;
    }
}
```

### `Config/RateLimiterConfig.java`
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter.Config;

public class RateLimiterConfig {
    private final int capacity;
    private final int refillRate;
    private final int maxRequests;
    private final Long windowSizeSeconds;

    public RateLimiterConfig(int capacity, int refillRate, int maxRequests, Long windowSizeSeconds) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.maxRequests = maxRequests;
        this.windowSizeSeconds = windowSizeSeconds;
    }

    public int getCapacity() { return capacity; }
    public int getRefillRate() { return refillRate; }
    public int getMaxRequests() { return maxRequests; }
    public Long getWindowSizeSeconds() { return windowSizeSeconds; }
}
```

### `RateLimiterFactory.java`
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Config.RateLimiterConfig;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Enum.AlgorithmType;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.FixedWindow;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.RateLimiter;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.TokenBucket;

public class RateLimiterFactory {
    public static RateLimiter createRateLimiter(AlgorithmType algorithmType, RateLimiterConfig config) {
        switch (algorithmType) {
            case FIXED_WINDOW:
                return new FixedWindow(config.getMaxRequests(), config.getWindowSizeSeconds());
            case TOKEN_BUCKET:
                return new TokenBucket(config.getCapacity(), config.getRefillRate());
            default:
                throw new IllegalArgumentException("Unknown algorithm type: " + algorithmType);
        }
    }
}
```

### `RateLimiterManager.java`
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Enum.UserType;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.RateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterManager {
    private final Map<UserType, RateLimiter> tierToLimiter;
    private final Map<String, UserType> userToTier;

    public RateLimiterManager() {
        this.tierToLimiter = new ConcurrentHashMap<>();
        this.userToTier = new ConcurrentHashMap<>();
    }

    public void registerLimiter(UserType tier, RateLimiter limiter) {
        tierToLimiter.put(tier, limiter);
    }

    public void registerUser(String userId, UserType tier) {
        userToTier.put(userId, tier);
    }

    public boolean allowRequest(Request request) {
        String userId = request.getUserId();
        UserType userType = userToTier.get(userId);

        if (userType == null) {
            return false; // fail closed — unregistered users rejected
        }

        RateLimiter limiter = tierToLimiter.get(userType);
        return limiter.allowRequest(request);
    }
}
```

### `RateLimiterClient.java` (final wiring)
```java
package com.example.interviewQuestions.LLD.Code.RateLimiter;

import com.example.interviewQuestions.LLD.Code.RateLimiter.Config.RateLimiterConfig;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Enum.AlgorithmType;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Enum.UserType;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Request.Request;
import com.example.interviewQuestions.LLD.Code.RateLimiter.Strategy.RateLimiter;

public class RateLimiterClient {
    public static void main(String[] args) {

        // 1. Config per tier
        RateLimiterConfig freeConfig = new RateLimiterConfig(5, 1, 0, 0L);       // 5 capacity, 1 token/sec
        RateLimiterConfig premiumConfig = new RateLimiterConfig(100, 10, 0, 0L); // 100 capacity, 10 tokens/sec

        // 2. Factory builds concrete algorithm objects — ONCE, at setup
        RateLimiter freeLimiter = RateLimiterFactory.createRateLimiter(AlgorithmType.TOKEN_BUCKET, freeConfig);
        RateLimiter premiumLimiter = RateLimiterFactory.createRateLimiter(AlgorithmType.TOKEN_BUCKET, premiumConfig);

        // 3. Manager + registration
        RateLimiterManager manager = new RateLimiterManager();
        manager.registerLimiter(UserType.FREE, freeLimiter);
        manager.registerLimiter(UserType.PREMIUM, premiumLimiter);

        manager.registerUser("user_free_1", UserType.FREE);
        manager.registerUser("user_premium_1", UserType.PREMIUM);

        // 4. Simulate requests
        Request freeReq = new Request("user_free_1", "127.0.0.1");
        for (int i = 1; i <= 7; i++) {
            boolean allowed = manager.allowRequest(freeReq);
            System.out.println("Free user request #" + i + " -> " + (allowed ? "ALLOWED" : "REJECTED (429)"));
        }

        Request premiumReq = new Request("user_premium_1", "127.0.0.2");
        System.out.println("Premium user request -> " +
                (manager.allowRequest(premiumReq) ? "ALLOWED" : "REJECTED (429)"));

        // 5. Unregistered user — fail closed
        Request unknownReq = new Request("unknown_user", "127.0.0.3");
        System.out.println("Unregistered user request -> " +
                (manager.allowRequest(unknownReq) ? "ALLOWED" : "REJECTED (429)"));
    }
}
```

**Expected behavior when run:**
- `user_free_1` (capacity 5, slow refill) — first ~5 rapid-fire requests print `ALLOWED`, remaining print `REJECTED (429)`, since tokens deplete faster than they refill in a tight loop.
- `user_premium_1` (capacity 100) — request easily `ALLOWED`.
- `unknown_user` — never registered, `REJECTED (429)` via fail-closed logic.

---

*End of notes.*