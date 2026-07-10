# Spring Boot Mock Interview (2–5 YOE) — Review Notes

A record of a 5-question mock interview covering Spring Boot fundamentals, with corrected answers and specific feedback on what was said incorrectly.

---

## Q1: `@Bean` vs `@Component`, `@Autowired` vs Constructor Injection

**Question:** When you define a `@Bean` and also use `@Component`, and a bean depends on another bean — what's the difference between `@Autowired` and constructor injection in terms of testability and immutability?

**Correct Answer:**
- `@Component` — for classes *you own*; auto-detected via component scanning.
- `@Bean` — inside a `@Configuration` class, used for classes you *don't own* (third-party/JAR classes) or when object creation needs custom logic.
- **Field injection** (`@Autowired` on a field) — Spring injects via reflection, bypassing the constructor. Fields can't be `final` (no immutability), and it's hard to unit test without a full Spring context.
- **Constructor injection** — dependencies passed via the constructor, can be `final`, easily testable with plain `new MyClass(mock)`, no Spring context required. **This is the recommended default.** If there's only one constructor, `@Autowired` isn't even needed (Spring 4.3+).

**What was said incorrectly:**
- Justification for using constructor injection was inconsistent — reasoning included "if I don't need to write manual code I'll use `@Autowired`," which isn't a valid justification. The correct reasoning is testability + immutability, and it should be the default choice, not situational.

---

## Q2: Request Flow / Filter vs Interceptor vs `@ControllerAdvice`

**Question:** Walk through what happens when a request hits a `@RestController`, from `DispatcherServlet` to response. Where would you use a filter vs interceptor vs `@ControllerAdvice`?

**Correct Answer (order matters):**
1. Servlet Container (Tomcat) receives the request
2. **Filters** run (Servlet-spec level, outside Spring, no knowledge of the handler) — auth, CORS, logging
3. **DispatcherServlet** takes over
4. `HandlerMapping` determines the target controller method
5. **Interceptors** `preHandle()` runs (Spring-specific, has access to the handler)
6. Controller executes → calls service/business logic
7. **`@ControllerAdvice`** catches exceptions globally, if any are thrown
8. Interceptor `postHandle()` / `afterCompletion()` runs
9. Response is serialized (JSON via `HttpMessageConverter`)
10. Response passes back out through filters to the client

**What was said incorrectly:**
- Claimed the client sends the request "to the interceptor" first — incorrect. Filters run first, at the servlet level, before Spring/DispatcherServlet is even involved. Interceptors only run *inside* the DispatcherServlet flow.
- Justification for interceptor was "it's inbuilt" — not a real differentiator. The actual distinction is whether Spring/handler context is needed (interceptor) vs raw servlet-level processing before Spring engages (filter).
- `@ControllerAdvice` was treated as part of the routing/interception chain — it isn't; it's purely for centralized exception handling.

---

## Q3: Self-Invocation and `@Transactional`

**Question:** Does `@Transactional` on a method still apply if it's called from another method *within the same class*, which is also `@Transactional`?

**Correct Answer:**
Spring's `@Transactional` works via **proxying**. External calls go through the proxy, which manages the transaction. A call from *within the same class* (`this.methodB()`) bypasses the proxy entirely, so the inner `@Transactional` is silently ignored.

**Fixes:**
1. Move the method to a separate `@Service` class and inject it.
2. Self-injection using `@Lazy` to inject the class's own proxy into itself.
3. `AopContext.currentProxy()` with `exposeProxy = true` (less common, considered a bit hacky).

**What was said incorrectly:**
- Nothing factually wrong — this was the strongest answer overall. Proxy bypass was correctly identified along with two valid fixes.
- Minor note: the key term "proxy" wasn't stated until late in the answer — lead with it next time.

---

## Q4: Transaction Propagation — `REQUIRED` vs `REQUIRES_NEW` vs `NESTED`

**Question:** Explain the propagation types and give a real scenario for choosing `REQUIRES_NEW` over `REQUIRED`.

**Correct Answer:**
- **`REQUIRED`** (default) — joins an existing transaction, or creates one if none exists. Outer rollback = inner rollback.
- **`REQUIRES_NEW`** — **suspends** (not terminates) any existing transaction, starts a fully independent new one, then **resumes** the original afterward. Inner rollback does not affect the outer transaction.
- **`NESTED`** — uses a **savepoint** within the same physical transaction; can roll back to that savepoint independently, but is still bound to the outer transaction's ultimate commit/rollback.
- **`MANDATORY`** — throws an exception if no transaction exists; never creates one itself.

**Real scenario for `REQUIRES_NEW`:** Audit logging — you want a log entry ("order attempt failed") to persist even if the main `placeOrder()` transaction rolls back, since it's independent.

**What was said incorrectly:**
- ❌ Stated `REQUIRES_NEW` "**terminates**" the existing transaction — incorrect. It **suspends** it and resumes it afterward. This is a factual, not just semantic, error.
- `NESTED` wasn't mentioned at all; `MANDATORY` was defined instead (not asked, though not wrong to bring up).
- The example given ("ensure ACID properties, use REQUIRES_NEW") was vague — a concrete case like audit logging would be stronger.

---

## Q5: `@RequestParam` vs `@PathVariable` vs `@RequestBody`, and JSON Deserialization

**Question:** Explain the differences, and how Spring deserializes JSON into a Java object — including what happens if the JSON has a field not present in the DTO.

**Correct Answer:**
- **`@PathVariable`** — extracted from the URL path (e.g., `/users/{id}`)
- **`@RequestParam`** — extracted from query string (`?name=John`) or form data
- **`@RequestHeader`** — extracted from HTTP headers (a separate, distinct annotation)
- **`@RequestBody`** — deserializes the JSON body into a DTO via Jackson's `ObjectMapper.readValue()`, triggered through `MappingJackson2HttpMessageConverter`
- **`@RestController`** = `@Controller` + `@ResponseBody` — governs the **response** side (serializing output to JSON), not deserialization of input.
- **Unknown JSON fields:** Jackson **ignores them silently by default** — it does **not** throw an exception unless explicitly configured with `FAIL_ON_UNKNOWN_PROPERTIES = true` or `@JsonIgnoreProperties(ignoreUnknown = false)`.

**What was said incorrectly:**
- ❌ Stated `@RequestParam` is used for headers — incorrect; that's `@RequestHeader`, a separate annotation entirely.
- ❌ Stated an unknown JSON field "will fail" — **backwards**. Default Jackson behavior is to silently ignore unknown fields, not throw. This is a classic interview gotcha designed to catch exactly this misconception.

---

## Summary: Hard Factual Errors to Fix

1. **`REQUIRES_NEW` suspends**, it does not terminate, other transactions.
2. **`@RequestHeader`** (not `@RequestParam`) is the annotation for HTTP headers.
3. **Jackson ignores unknown JSON fields by default** — it does not fail unless explicitly configured to.

## Overall Assessment

- Conceptual understanding is solid for a 2–5 YOE candidate — proxy/self-invocation, propagation types, filter vs interceptor, and DTO deserialization were all fundamentally understood.
- The biggest risk is **communication clarity**, not knowledge — answers were often run-on, self-corrected mid-thought, and buried the key term/verdict instead of leading with it.
- Recommended fix: structure answers as **keyword/verdict first, then explanation** in 2–3 sentences before elaborating further. E.g., *"Self-invocation bypasses the Spring proxy, so the inner `@Transactional` is ignored. That's because Spring uses proxy-based AOP..."*
- Fixing the three factual errors above, combined with tighter delivery, would likely move this from borderline to a clear pass.
