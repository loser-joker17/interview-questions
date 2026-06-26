# Java Backend Interview Notes - OOP, SOLID & Design Patterns

# Question 1: Composition vs Inheritance

## Scenario

A notification system initially supports:

* Email
* SMS

Later the business adds optional capabilities:

* Encryptable
* Retryable
* Compressible
* RateLimited

---

## Question

Would you continue using inheritance?

```java
class EncryptedEmailNotification
class RetryableEmailNotification
class CompressedEncryptedRetryableEmailNotification
```

Or would you switch to composition?

---

## Expected Answer

Prefer **composition**.

### Why?

Inheritance causes:

* Tight coupling
* Class explosion
* Difficult maintenance

Instead:

```java
Notification notification = new EmailNotification();

notification = new EncryptionDecorator(notification);
notification = new RetryDecorator(notification);
notification = new CompressionDecorator(notification);
```

Benefits:

* Dynamic behavior composition
* Open/Closed Principle
* Better maintainability

---

# Question 2: Liskov Substitution Principle (LSP)

## Scenario

```java
class Bird {
    void fly() {}
}

class Sparrow extends Bird {}

class Penguin extends Bird {

    @Override
    void fly() {
        throw new UnsupportedOperationException();
    }
}
```

---

## Why is this wrong?

A Penguin cannot replace a Bird if the client expects every Bird to fly.

This violates the **Liskov Substitution Principle**.

---

## Better Design

```java
abstract class Bird {
    abstract void eat();
}

interface Flyable {
    void fly();
}

interface Swimmable {
    void swim();
}
```

```java
class Sparrow extends Bird implements Flyable {}
class Penguin extends Bird implements Swimmable {}
```

---

# Question 3: Dependency Inversion Principle (DIP)

## Bad Design

```java
class PaymentService {

    private RazorpayGateway gateway =
            new RazorpayGateway();
}
```

---

## Why?

High-level modules should not depend on concrete implementations.

---

## Better Design

```java
interface PaymentGateway {
    void pay();
}
```

```java
class RazorpayGateway implements PaymentGateway {}
```

```java
class PaymentService {

    private final PaymentGateway gateway;

    public PaymentService(PaymentGateway gateway) {
        this.gateway = gateway;
    }
}
```

Spring injects the implementation.

---

# Question 4: Factory Pattern

## Scenario

Payment methods:

* Razorpay
* Stripe
* PayPal

Client sends:

```
RAZORPAY
STRIPE
PAYPAL
```

---

## Which Pattern?

Factory Pattern.

Why?

Avoid:

```java
if(...)
else if(...)
```

Create objects based on runtime input.

---

# Question 5: Factory Pattern Challenge

Suppose the business keeps adding gateways every month.

Eventually your factory contains:

```java
switch(method) {
    ...
}
```

with 50 cases.

---

## Problem

Factory itself violates the Open/Closed Principle.

---

## Better Spring Solution

```java
Map<String, PaymentGateway> gateways;
```

Spring automatically injects all implementations.

Runtime:

```java
PaymentGateway gateway =
        gateways.get(method);

gateway.pay();
```

No switch.

No modification.

---

# Question 6: Strategy Pattern

## Scenario

Ride Fare Calculation

* Bike
* Auto
* Cab
* Premium Cab

Each has a different algorithm.

---

## Solution

```java
interface FareStrategy {
    double calculateFare(Ride ride);
}
```

Implementations:

* BikeFareStrategy
* AutoFareStrategy
* CabFareStrategy
* PremiumFareStrategy

Runtime:

```java
strategy.calculateFare(ride);
```

---

## Why Strategy?

Because the algorithm changes, not the object.

---

# Question 7: When NOT to Use Strategy

Suppose there are only:

* Bike
* Cab

Calculation:

```java
if(type == BIKE)
    return distance * 10;

return distance * 15;
```

---

## Should Strategy Be Used?

No.

It becomes over-engineering.

Good engineers introduce patterns only when they solve a real problem.

Rule:

> Keep it simple until complexity justifies abstraction.

---

# Question 8: Delivery Partner Design

## Scenario

Every delivery partner has:

* partnerId
* name
* rating

Optional capabilities:

* Accept Cash
* Deliver Alcohol
* Carry Heavy Items
* Express Delivery

---

## Correct Design

Shared state:

```java
abstract class DeliveryPartner
```

Capabilities:

```java
interface CashAcceptable
interface AlcoholDeliverable
interface HeavyItemDeliverable
interface ExpressDeliverable
```

Example:

```java
class BikePartner
    extends DeliveryPartner
    implements CashAcceptable,
               ExpressDeliverable {
}
```

---

## Why Not Abstract Classes?

Java doesn't support multiple inheritance.

Interfaces allow combining multiple capabilities.

---

# Important Interview Lessons

## Use Interface When

* Modeling capabilities
* Optional behavior
* Multiple inheritance is required

Examples:

* Runnable
* Comparable
* Refundable
* Retryable

---

## Use Abstract Class When

* Shared state exists
* Common implementation exists
* Constructors are needed

Examples:

* Payment
* Vehicle
* Animal
* DeliveryPartner

---

# Design Pattern Selection

| Situation                              | Pattern         |
| -------------------------------------- | --------------- |
| Object creation based on runtime input | Factory         |
| Different algorithms                   | Strategy        |
| Add behaviors dynamically              | Decorator       |
| Fixed workflow with customizable steps | Template Method |

---

# Golden Rules

* Prefer composition over inheritance.
* Program to interfaces, not implementations.
* Use abstraction only when it solves a real problem.
* Avoid over-engineering.
* Follow the Open/Closed Principle.
* Use Spring Dependency Injection instead of manually creating dependencies.
* Design patterns are tools, not goals.

---

