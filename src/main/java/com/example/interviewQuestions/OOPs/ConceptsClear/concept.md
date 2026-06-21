# OOP Interview Notes: Interface vs Abstract Class

## 1. Why can't we create an object of an Interface?

### Example

```java
interface Animal {
    void sound();
}
```

```java
Animal a = new Animal(); // Compilation Error
```

### Reason

An interface defines a contract but does not provide complete implementation for its abstract methods.

Java does not know what code should execute when:

```java
a.sound();
```

### Interview Answer

> We cannot instantiate an interface because it may contain abstract methods without implementations. Java requires a complete implementation before creating an object.

---

# 2. Why can't we create an object of an Abstract Class?

### Example

```java
abstract class Animal {
    abstract void sound();
}
```

```java
Animal a = new Animal(); // Compilation Error
```

### Reason

Abstract classes can contain abstract methods that have no implementation.

The class is considered incomplete.

### Interview Answer

> An abstract class cannot be instantiated because it may contain abstract methods that are not implemented. Only concrete subclasses can be instantiated.

---

# 3. Empty Method vs No Implementation

### Valid

```java
class Animal {
    void sound() {
    }
}
```

```java
Animal a = new Animal();
```

### Invalid

```java
class Animal {
    void sound();
}
```

### Key Point

```java
void sound() {}
```

is an implementation.

```java
void sound();
```

is not.

---

# 4. Interface vs Abstract Class

## Interface

Represents a capability or contract.

Examples:

```java
interface Flyable {}
interface Runnable {}
interface Refundable {}
interface Auditable {}
```

Think:

```text
Can Do
```

---

## Abstract Class

Represents a common base with shared state and behavior.

Examples:

```java
abstract class Animal {}
abstract class Vehicle {}
abstract class Payment {}
```

Think:

```text
Is A
```

---

# 5. What is Object State?

State = Data stored inside an object.

```java
class Dog {
    String name;
    int age;
}
```

Example:

```text
Dog1 -> Tommy, 2
Dog2 -> Bruno, 5
```

The values of name and age are the object's state.

---

# 6. Why Use Abstract Class?

When classes share:

* State
* Common implementation
* Constructors

Example:

```java
abstract class Payment {

    String paymentId;
    BigDecimal amount;
    Instant createdTime;

    abstract void processPayment();
}
```

---

# 7. Why Use Interface?

When modeling capabilities.

Example:

```java
interface Refundable {
    void refund();
}

interface Auditable {
    void audit();
}
```

A payment may be:

* Refundable
* Auditable
* Both
* Neither

Interfaces provide flexibility.

---

# 8. Why Not Use Abstract Classes for Everything?

Java supports:

```java
class A extends B
```

but not:

```java
class A extends B, C
```

No multiple inheritance of classes.

Example:

```java
abstract class Refundable {}
abstract class Auditable {}
```

Then:

```java
class CreditCardPayment
    extends Refundable, Auditable
```

is impossible.

Interfaces solve this problem.

---

# 9. Runtime Polymorphism

Example:

```java
Refundable refund = new CreditCardPayment();

refund.refund();
```

or

```java
Refundable refund = new UPIPayment();

refund.refund();
```

Same method call.

Different behavior.

### Interview Answer

> Runtime polymorphism allows the JVM to decide which implementation to execute based on the actual object type at runtime.

---

# 10. Open Closed Principle (OCP)

## Bad

```java
if(payment instanceof CreditCardPayment) {
}
else if(payment instanceof UPIPayment) {
}
else if(payment instanceof NetBankingPayment) {
}
```

Every new payment type requires modifying existing code.

OCP violation.

---

## Good

```java
payment.processPayment();
```

Use polymorphism.

New payment types can be added without modifying existing code.

---

# Situational Question 1: Payment System Design

## Scenario

Initially:

* Credit Card Payment
* UPI Payment
* Net Banking Payment

All payments have:

```text
paymentId
amount
createdTime
status
```

Later business adds:

```text
Refundable
Auditable
```

Some payment types support one capability, some both.

---

## Recommended Design

### Abstract Class

```java
abstract class Payment {

    String paymentId;
    BigDecimal amount;
    Instant createdTime;
    PaymentStatus status;

    abstract void processPayment();
}
```

### Interfaces

```java
interface Refundable {
    void refund();
}

interface Auditable {
    void audit();
}
```

### Implementations

```java
class CreditCardPayment
        extends Payment
        implements Refundable, Auditable {
}
```

```java
class UPIPayment
        extends Payment
        implements Auditable {
}
```

```java
class NetBankingPayment
        extends Payment
        implements Refundable {
}
```

### Why?

* Shared state → Abstract Class
* Optional capabilities → Interfaces
* Avoids multiple inheritance issue
* Supports OCP

---

# Situational Question 2: Notification System Design

## Scenario

Notifications:

* Email
* SMS
* WhatsApp
* Push
* Slack

Capabilities:

```text
Schedulable
Retryable
Encryptable
```

Examples:

```text
Email -> Schedulable + Retryable + Encryptable
SMS -> Retryable
Push -> Schedulable
WhatsApp -> Encryptable
Slack -> None
```

---

## Recommended Design

### Notification Contract

```java
interface Notification {
    void send();
}
```

### Shared State

```java
abstract class BaseNotification
        implements Notification {

    String notificationId;
    Instant createdAt;
    String recipient;
    Status status;
}
```

### Capabilities

```java
interface Schedulable {
    void schedule();
}
```

```java
interface Retryable {
    void retry();
}
```

```java
interface Encryptable {
    void encrypt();
}
```

### Example

```java
class EmailNotification
        extends BaseNotification
        implements Retryable,
                   Encryptable,
                   Schedulable {
}
```

### Why?

* Shared data → Abstract Class
* Optional behavior → Interfaces
* Multiple capabilities supported
* OCP compliant

---

# Advanced Design Pattern Discussion

When capabilities keep increasing:

```text
Encryptable
Retryable
Compressible
VirusScannable
RateLimited
```

Avoid:

```java
if(...)
if(...)
if(...)
```

and avoid creating dozens of classes.

Preferred Pattern:

## Decorator Pattern

```java
Notification email =
        new EmailNotification();

email =
        new EncryptionDecorator(email);

email =
        new RetryDecorator(email);

email.send();
```

### Benefits

* Open Closed Principle
* Dynamic behavior composition
* No class explosion

---

# Final Interview Summary

## Use Interface When

* Modeling capabilities
* Multiple inheritance is needed
* No shared state

Examples:

```java
Runnable
Comparable
Refundable
Auditable
```

---

## Use Abstract Class When

* Shared state exists
* Common implementation exists
* Constructors are needed

Examples:

```java
Animal
Vehicle
Payment
BaseNotification
```

---

## Golden Rule

**Interface = What an object CAN DO**

**Abstract Class = What an object IS**
