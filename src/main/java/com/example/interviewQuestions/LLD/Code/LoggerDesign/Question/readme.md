# Logging Framework — LLD Mock Interview Notes

A full record of the mock interview: requirements gathering, design decisions,
mistakes made, corrections applied, follow-up questions, and the final working code.

---

## 1. Requirements Gathering

### Functional Requirements (FRs)

1. The system should support logging messages at different **levels** with a defined
   hierarchy/rank: `TRACE < DEBUG < INFO < WARN < ERROR < FATAL`.
2. Each logger should support a configurable **threshold** — only messages with
   rank **≥ threshold rank** are recorded/processed.
3. The system should support multiple **appenders/destinations** — Console, File,
   Remote server/Database — that logs can be sent to.
4. A single logger should be able to write to **multiple destinations simultaneously**
   (e.g., Console + File at once).
5. The log message **format should be configurable** — user can define which fields
   appear (timestamp, level, message, class/method name, thread name, etc.).

### Non-Functional Requirements (NFRs)

1. **Performance** — logging should not block/slow down the main application's
   business logic (ideally async/non-blocking I/O).
2. **Thread-safety** — concurrent logging must not corrupt or interleave output,
   especially when multiple threads write to a shared resource (file, DB connection).
3. **Extensibility** — adding a new appender/formatter should not require modifying
   existing code (**Open/Closed Principle**).

### Key clarifying Q&A during requirements

| Question | Answer / Reasoning |
|---|---|
| Single log file or separate files per level? | Left open — resolved by making appenders pluggable, so this becomes a config choice, not a hardcoded one. |
| Why do log levels need an ordered hierarchy instead of independent tags? | So a single **threshold** value can filter out an entire category of noise at once (e.g., threshold = WARN silently drops DEBUG/INFO) instead of toggling each level individually. |
| Should destination be fixed to "file"? | No — must be **pluggable** (Console/File/DB/Remote), because a *framework* should not hardcode where output goes. |
| Should format be fixed in code? | No — must be **configurable** by the user of the framework. |

---

## 2. Design Evolution

### First pass at classes (had issues)

Initial list: `Level` (as a class, not enum), `AppenderManager`, enums for
`levelType`, `messageState`, `appenderType`. **No `Logger` class at all.**

**Problems identified:**
- Missing the most central class — `Logger` — the object the developer actually calls `.info()`/`.error()` on.
- `Level` didn't need to be its own class; an `enum` with ordinal-based severity is enough.
- `messageState (process, recorded)` was unnecessary complexity — a message is either filtered out or written; no formal state machine needed.
- No `Formatter` and no `LogMessage`/`LogEvent` data-carrier object.
- Unclear where the **threshold check** should live.

### Final agreed architecture

```
Logger  ──has-a──>  AppendManager  ──has-a (1:N)──>  List<Appender>
  |                                                       |
  | builds                                     each Appender has-a Formatter
  ▼
LogMessage (timestamp, level, message)
```

- **`Logger`** — checks threshold, builds `LogMessage`, delegates to `AppendManager`.
- **`AppendManager`** — holds a `List<Appender>`, exposes `addAppender()`, loops and delegates writes. (Open/Closed: new appenders = new classes + one registration line, zero changes here.)
- **`Appender`** (interface) — `append(LogMessage)`, implemented by `ConsoleAppender`, `FileAppender`, `DatabaseAppender`. Each holds its own injected `Formatter`.
- **`Formatter`** (interface) — `formatMessage(LogMessage)`, implemented by `SimpleFormatter`, `JsonFormatter`. Swappable per-appender.
- **`LogMessage`** — plain data carrier (level, message, timestamp).
- **`LevelType`** (enum) — `TRACE(0) < DEBUG(1) < INFO(2) < WARN(3) < ERROR(4) < FATAL(5)`, ordinal = severity rank.

**Threshold rule:** a message is processed only if `level.getSeverity() >= threshold.getSeverity()`.

---

## 3. Bugs Found in the Code — Root Cause and Fix

All four bugs traced back to **one repeating root cause**:

> **A field/parameter/dependency was declared (constructor param, injected object) but the method body didn't actually use it — it hardcoded something else instead, or discarded a return value.**

### Bug #1 — `AppendManager` constructor mismatch / wrong shape

**Before:**
```java
public AppendManager(Appender appender, Formatter formatter) { ... }
```
Client called `new AppendManager()` with no args → wouldn't compile. The single-appender,
single-formatter constructor also didn't match the requirement of "multiple pluggable appenders."

**Fix:** `AppendManager` should hold a `List<Appender>`, filled in from outside via `addAppender()`:
```java
public class AppendManager {
    private final List<Appender> appenders = new ArrayList<>();
    public void addAppender(Appender appender) { appenders.add(appender); }
    public void append(LogMessage logMessage) {
        for (Appender appender : appenders) appender.append(logMessage);
    }
}
```

### Bug #2 — `AppendManager.append()` hardcoded 3 appenders internally

**Before:**
```java
public void append(LogMessage logMessage){
    Appender consoleAppender = new ConsoleAppender(formatter);
    consoleAppender.append(logMessage);
    Appender databaseAppender = new DatabaseAppender(formatter);
    databaseAppender.append(logMessage);
    Appender fileAppender = new FileAppender(formatter);
    fileAppender.append(logMessage);
}
```
Problems: recreated appender objects on every call; hardcoded to always use exactly 3 appenders
(violates OCP — adding a 4th means editing this method); the constructor's `appender` field was
never used at all (dead code).

**Fix:** see Bug #1's fix above — appenders are registered once via `addAppender()`, then the
loop in `append()` just iterates whatever was registered. No `new` inside a Manager class.

### Bug #3 — `Logger.log()` discarded the real level

**Before:**
```java
public void log(LevelType level, String message){
    if(level.getSeverity() < threshold.getSeverity()){ return; }
    LogMessage logMessage = new LogMessage(LevelType.INFO, message, LocalDateTime.now());
    appendManager.append(logMessage);
}
```
The threshold check correctly used `level`, but the `LogMessage` object was always built with
the hardcoded literal `LevelType.INFO` — so a `FATAL` call would still be recorded as `INFO`.

**Fix:**
```java
LogMessage logMessage = new LogMessage(level, message, LocalDateTime.now());
```

### Bug #4 — Appenders ignored their injected `Formatter`, and discarded return values

**Before (repeated in `ConsoleAppender`, `FileAppender`, `DatabaseAppender`):**
```java
public void append(LogMessage message){
    Formatter simple = new SimpleFormatter();
    simple.formatMessage(message);          // return value thrown away — nothing prints
    Formatter json = new JsonFormatter();
    json.formatMessage(message);            // same issue
}
```
The constructor took a `formatter` field but the method never used `this.formatter` — instead
it hardcoded two other formatters and never used their output.

**Fix:**
```java
@Override
public void append(LogMessage message){
    String formatted = formatter.formatMessage(message);
    System.out.println(formatted);
}
```

### The general rule that catches all four

> If a class has a constructor parameter or field for something, that field must only ever be
> set from outside — never re-created with `new` inside a method of the same class.
> After writing any method: (1) scan for `new SomeType(...)` where `SomeType` is also a field of
> the class — that's a bug; (2) check every method call whose return value should be used is
> actually assigned/used, not discarded.

---

## 4. The "Client Wiring" Pattern (Dependency Injection Order)

A recurring point of confusion was **how to wire everything together in `main()`**. The rule:

> **A Manager class should never contain `new SomeConcreteClass()` inside itself.** The
> Manager's job is to *hold and orchestrate* what it's given, not *create* what it holds.
> The **client** (`main()`/config code) is the one place allowed to know about concrete classes.

**Bottom-up wiring order** (works for almost any LLD problem):

1. Build leaf objects first (no dependencies) — e.g. `Formatter` implementations.
2. Build objects that depend on those leaves — e.g. `Appender` implementations.
3. Build the Manager, then register step 2's objects into it.
4. Build the top-level object last, injecting the Manager.
5. Call the actual operation.

```java
// 1. leaves
Formatter simpleFormatter = new SimpleFormatter();
Formatter jsonFormatter = new JsonFormatter();

// 2. things depending on leaves
Appender console = new ConsoleAppender(simpleFormatter);
Appender file = new FileAppender(jsonFormatter);

// 3. manager + registration
AppendManager appendManager = new AppendManager();
appendManager.addAppender(console);
appendManager.addAppender(file);

// 4. top-level object
Logger logger = new Logger(LevelType.INFO, appendManager);

// 5. use it
logger.log(LevelType.ERROR, "Something failed");
```

---

## 5. Follow-up / Extension Questions (Depth Round)

### Q1 — How do you support multiple loggers across a large app (one per class), sharing common appenders?

**Answer:** Introduce a `LoggerFactory` — a central place that creates and **caches** `Logger`
instances, keyed by class name, all sharing one `AppendManager` instance.

```java
class LoggerFactory {
    private static final Map<String, Logger> loggerCache = new HashMap<>();
    private static final AppendManager sharedAppendManager = new AppendManager();

    public static Logger getLogger(String className) {
        if (loggerCache.containsKey(className)) {
            return loggerCache.get(className);
        }
        Logger newLogger = new Logger(LevelType.INFO, sharedAppendManager);
        loggerCache.put(className, newLogger);
        return newLogger;
    }
}
```

Usage: `private static final Logger logger = LoggerFactory.getLogger("UserService");`

**Why caching matters (not just performance):** without it, every call would return a fresh
object with default config, silently discarding anything like a runtime `setThreshold()` change
made earlier — each class needs a **stable identity** for its logger.

### Q2 — Two threads write to the same file appender at once — what breaks, and how do you fix it?

**Risk:** log lines from different threads can **interleave** mid-write, corrupting the file
(partial lines mixed together) instead of two clean, separate lines.

**Fix — precise locking, not broad locking.** Do **not** put `synchronized` on the whole
`Logger.log()` method (that would block Console/DB writers just because the file writer is busy).
Instead, lock only at the point where the actual shared resource is touched:

```java
public class FileAppender implements Appender {
    @Override
    public synchronized void append(LogMessage message) {
        String formatted = formatter.formatMessage(message);
        // write "formatted" to file
    }
}
```

**Principle:** lock at the narrowest point where the shared, mutable resource is actually used —
not at the entry point of the whole operation. Broad locking hurts performance for no benefit;
narrow locking keeps unrelated appenders (Console, DB) running independently.

### Q3 — Does the design support per-appender thresholds (e.g., Console shows ERROR+, File shows DEBUG+)?

**Answer:** Not by default — `Logger`'s threshold check is a single global gate *before* the
message reaches any appender, so if it passes, all appenders get it; if it fails, none do.

**Fix:** give each `Appender` its own `threshold` field and check it again inside `append()`:

```java
public class ConsoleAppender implements Appender {
    private final Formatter formatter;
    private final LevelType threshold;

    public ConsoleAppender(Formatter formatter, LevelType threshold) {
        this.formatter = formatter;
        this.threshold = threshold;
    }

    @Override
    public void append(LogMessage message) {
        if (message.getLevel().getSeverity() < threshold.getSeverity()) {
            return;
        }
        String formatted = formatter.formatMessage(message);
        System.out.println(formatted);
    }
}
```

This gives **two layers of filtering**: `Logger`'s global minimum, and each `Appender`'s own
per-destination override — matching how Log4j actually works.

### Q4 — How would you make `logger.log()` non-blocking (async logging)?

**Pattern: Producer–Consumer**, using a `BlockingQueue`.

- **Producer** = the calling application thread. `Logger.log()` just drops the `LogMessage`
  onto a queue and returns immediately — it no longer calls `AppendManager` directly.
- **Consumer** = a dedicated background thread that continuously pulls messages off the queue
  and performs the actual (slow) `AppendManager.append(...)` I/O work.

```java
class Logger {
    private final LevelType threshold;
    private final BlockingQueue<LogMessage> queue;

    public Logger(LevelType threshold, BlockingQueue<LogMessage> queue) {
        this.threshold = threshold;
        this.queue = queue;
    }

    public void log(LevelType level, String message) {
        if (level.getSeverity() < threshold.getSeverity()) return;
        LogMessage logMessage = new LogMessage(level, message, LocalDateTime.now());
        queue.offer(logMessage);   // fast — no I/O here
    }
}

class LogConsumerWorker implements Runnable {
    private final BlockingQueue<LogMessage> queue;
    private final AppendManager appendManager;

    public LogConsumerWorker(BlockingQueue<LogMessage> queue, AppendManager appendManager) {
        this.queue = queue;
        this.appendManager = appendManager;
    }

    @Override
    public void run() {
        while (true) {
            LogMessage message = queue.take();     // blocks until something arrives
            appendManager.append(message);          // slow I/O happens here, off the caller's thread
        }
    }
}
```

Wiring: create the queue and `AppendManager` once, start one background `Thread` running
`LogConsumerWorker`, then every `logger.log(...)` call returns almost instantly.

### Q5 — How would you make configuration (levels, appenders, formats) file-driven instead of hardcoded Java?

**Pattern: Builder or Factory**, reading from an external config (properties/XML/YAML) at
startup to construct the `Logger`/`AppendManager`/`Appender` graph, instead of wiring it by hand
in `main()`. E.g. a `LoggerConfigLoader` parses `log4j.properties`-style entries such as
`appender.console.threshold=ERROR`, `appender.file.formatter=json`, and uses a `Builder` to
assemble the matching objects — so changing behavior means editing a config file, not code.

### Q6 — How would you support log file rotation (daily, or over 10MB)?

**Where it lives:** inside `FileAppender` (or a small helper it delegates to), **not** in
`Logger` or `Formatter` — rotation is a concern specific to *how* the file destination manages
its own output, unrelated to filtering (Logger's job) or text formatting (Formatter's job).
`FileAppender.append()` would check, before writing, whether the current file has exceeded a
size threshold or whether the date has changed since the last write, and if so, close the
current file and open a new one (e.g. `app-2026-08-09.log`) before writing.

---

## 6. Self-Review Checklist (the actual weakness + how to fix it)

**Root weakness identified across this session:** strong at design (responsibilities,
interfaces, SOLID reasoning) but weak at **verifying that written code actually does what was
designed** — fields declared but not used, return values discarded, hardcoded values left in
place of parameters.

**Drill to build the missing habit:**
1. After writing any method, mentally trace **one concrete example** through it before moving on
   ("if I call `log(FATAL, 'x')`, what ends up in `LogMessage`? What does `ConsoleAppender` do
   with it?").
2. Treat "field declared but never referenced in a method body" as an automatic red flag.
3. Treat "`new SomeConcreteClass()` inside a class that also takes that type as a constructor
   parameter" as an automatic red flag (Manager classes should never construct their own
   dependencies).
4. Confirm every method call whose return value matters is actually captured and used.
5. In a live interview, narrate this trace out loud — interviewers give credit for visible
   self-review, even when it surfaces your own bug.

---

## 7. Final Working Code

### `Enums/LevelType.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums;

public enum LevelType {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5);

    private final int severity;

    LevelType(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }
}
```

### `LogMessage.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums.LevelType;
import java.time.LocalDateTime;

public class LogMessage {
    private final LevelType level;
    private final String message;
    private final LocalDateTime timestamp;

    public LogMessage(LevelType level, String message, LocalDateTime timestamp) {
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    public LevelType getLevel() { return level; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

### `Formatter/Formatter.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public interface Formatter {
    String formatMessage(LogMessage logMessage);
}
```

### `Formatter/SimpleFormatter.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class SimpleFormatter implements Formatter {
    @Override
    public String formatMessage(LogMessage logMessage) {
        return "[" + logMessage.getTimestamp() + "] "
                + logMessage.getLevel() + " - "
                + logMessage.getMessage();
    }
}
```

### `Formatter/JsonFormatter.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class JsonFormatter implements Formatter {
    @Override
    public String formatMessage(LogMessage logMessage) {
        return "{"
                + "\"timestamp\":\"" + logMessage.getTimestamp() + "\","
                + "\"level\":\"" + logMessage.getLevel() + "\","
                + "\"message\":\"" + logMessage.getMessage() + "\""
                + "}";
    }
}
```

### `appender/Appender.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign.appender;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public interface Appender {
    void append(LogMessage logMessage);
}
```

### `appender/ConsoleAppender.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign.appender;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.Formatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class ConsoleAppender implements Appender {
    private final Formatter formatter;

    public ConsoleAppender(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void append(LogMessage message) {
        String formatted = formatter.formatMessage(message);
        System.out.println(formatted);
    }
}
```

### `appender/FileAppender.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign.appender;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.Formatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class FileAppender implements Appender {
    private final Formatter formatter;

    public FileAppender(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public synchronized void append(LogMessage message) {
        String formatted = formatter.formatMessage(message);
        System.out.println("Message added to File: " + formatted);
        // in a real implementation: write `formatted` to a file on disk
    }
}
```

### `appender/DatabaseAppender.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign.appender;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.Formatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class DatabaseAppender implements Appender {
    private final Formatter formatter;

    public DatabaseAppender(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void append(LogMessage message) {
        String formatted = formatter.formatMessage(message);
        System.out.println("Message added to Database: " + formatted);
        // in a real implementation: insert `formatted` into a DB table
    }
}
```

### `AppendManager.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.Appender;

import java.util.ArrayList;
import java.util.List;

public class AppendManager {
    private final List<Appender> appenders = new ArrayList<>();

    public void addAppender(Appender appender) {
        appenders.add(appender);
    }

    public void append(LogMessage logMessage) {
        for (Appender appender : appenders) {
            appender.append(logMessage);
        }
    }
}
```

### `Logger.java`
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums.LevelType;
import java.time.LocalDateTime;

public class Logger {
    private final LevelType threshold;
    private final AppendManager appendManager;

    public Logger(LevelType threshold, AppendManager appendManager) {
        this.threshold = threshold;
        this.appendManager = appendManager;
    }

    public void log(LevelType level, String message) {
        if (level.getSeverity() < threshold.getSeverity()) {
            return;
        }
        LogMessage logMessage = new LogMessage(level, message, LocalDateTime.now());
        appendManager.append(logMessage);
    }
}
```

### `LoggerFactory.java` (Q1 extension)
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums.LevelType;

import java.util.HashMap;
import java.util.Map;

public class LoggerFactory {
    private static final Map<String, Logger> loggerCache = new HashMap<>();
    private static final AppendManager sharedAppendManager = new AppendManager();

    public static Logger getLogger(String className) {
        if (loggerCache.containsKey(className)) {
            return loggerCache.get(className);
        }
        Logger newLogger = new Logger(LevelType.INFO, sharedAppendManager);
        loggerCache.put(className, newLogger);
        return newLogger;
    }

    public static AppendManager getSharedAppendManager() {
        return sharedAppendManager;
    }
}
```

### `LoggerClient.java` (final wiring)
```java
package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums.LevelType;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.Formatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.JsonFormatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.SimpleFormatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.ConsoleAppender;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.DatabaseAppender;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.FileAppender;

public class LoggerClient {
    public static void main(String[] args) {

        // 1. leaves
        Formatter simpleFormatter = new SimpleFormatter();
        Formatter jsonFormatter = new JsonFormatter();

        // 2. things depending on leaves
        ConsoleAppender consoleAppender = new ConsoleAppender(simpleFormatter);
        FileAppender fileAppender = new FileAppender(jsonFormatter);
        DatabaseAppender databaseAppender = new DatabaseAppender(simpleFormatter);

        // 3. manager + registration
        AppendManager appendManager = new AppendManager();
        appendManager.addAppender(consoleAppender);
        appendManager.addAppender(fileAppender);
        appendManager.addAppender(databaseAppender);

        // 4. top-level object
        Logger logger = new Logger(LevelType.INFO, appendManager);

        // 5. use it
        logger.log(LevelType.INFO, "User Id is 123");
        logger.log(LevelType.DEBUG, "This will be dropped, below threshold");
        logger.log(LevelType.FATAL, "Disk full");
    }
}
```

**Verified output for `logger.log(LevelType.INFO, "User Id is 123")`:**
```
[2026-08-09T22:18:35.076271200] INFO - User Id is 123
Message added to File: {"timestamp":"2026-08-09T22:18:35.076271200","level":"INFO","message":"User Id is 123"}
Message added to Database: [2026-08-09T22:18:35.076271200] INFO - User Id is 123
```

---

*End of notes. Next: Rate Limiter LLD round.*