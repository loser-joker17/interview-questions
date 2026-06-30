# Composition with Interfaces in Java

## Problem Statement

Suppose we are building a notification system. Our application should be able to send notifications through different channels such as:

- Email
- SMS
- WhatsApp

Instead of writing separate business logic for every notification type, we use **Composition** along with an **Interface**.

---

# Step 1: Create the Interface

```java
public interface NotificationSender {
    void send(String message);
}
```

This interface defines a contract.

Any class implementing `NotificationSender` must provide its own implementation of the `send()` method.

---

# Step 2: Create Different Implementations

## Email Sender

```java
public class EmailSender implements NotificationSender {

    @Override
    public void send(String message) {
        System.out.println("Sending Email: " + message);
    }
}
```

---

## SMS Sender

```java
public class SmsSender implements NotificationSender {

    @Override
    public void send(String message) {
        System.out.println("Sending SMS: " + message);
    }
}
```

---

## WhatsApp Sender

```java
public class WhatsAppSender implements NotificationSender {

    @Override
    public void send(String message) {
        System.out.println("Sending WhatsApp Message: " + message);
    }
}
```

Each class provides its own implementation of the `send()` method.

---

# Step 3: Use Composition

```java
public class NotificationService {

    private NotificationSender sender;

    public NotificationService(NotificationSender sender) {
        this.sender = sender;
    }

    public void notifyUser(String message) {

        validateMessage(message);

        sender.send(message);

        logNotification();

        saveAudit();
    }

    private void validateMessage(String message) {
        System.out.println("Validating message...");
    }

    private void logNotification() {
        System.out.println("Logging notification...");
    }

    private void saveAudit() {
        System.out.println("Saving audit...");
    }
}
```

Notice something important.

`NotificationService` **does not know** whether it is sending:

- Email
- SMS
- WhatsApp

It only knows:

```java
sender.send(message);
```

This is called **Programming to an Interface**.

---

# Step 4: Main Method

## Using Email

```java
public class Main {

    public static void main(String[] args) {

        NotificationService service =
                new NotificationService(new EmailSender());

        service.notifyUser("Welcome!");
    }
}
```

### Output

```
Validating message...
Sending Email: Welcome!
Logging notification...
Saving audit...
```

---

## Tomorrow Business Changes

The business says:

> "Use WhatsApp instead of Email."

The only change required is:

```java
NotificationService service =
        new NotificationService(new WhatsAppSender());

service.notifyUser("Welcome!");
```

Everything else remains unchanged.

Output

```
Validating message...
Sending WhatsApp Message: Welcome!
Logging notification...
Saving audit...
```

---

# What if We Don't Use Composition?

Suppose we write:

```java
public class NotificationService {

    public void sendEmail(String message) {

        validateMessage(message);

        System.out.println("Sending Email");

        logNotification();

        saveAudit();
    }

    public void sendSMS(String message) {

        validateMessage(message);

        System.out.println("Sending SMS");

        logNotification();

        saveAudit();
    }

    public void sendWhatsApp(String message) {

        validateMessage(message);

        System.out.println("Sending WhatsApp");

        logNotification();

        saveAudit();
    }

    private void validateMessage(String message) {}

    private void logNotification() {}

    private void saveAudit() {}
}
```

### Problem

Notice that these methods all repeat the same workflow:

- Validate
- Log
- Save Audit

Only the actual sending logic changes.

This results in duplicated code, making maintenance difficult.

---

# Why Composition is Better

With composition:

```java
public void notifyUser(String message) {

    validateMessage(message);

    sender.send(message);

    logNotification();

    saveAudit();
}
```

The common business workflow exists **only once**.

Different notification channels provide only their own sending logic.

---

# Is It True That We Never Modify Code?

No.

Many developers misunderstand this principle.

Consider:

Today:

```java
NotificationService service =
        new NotificationService(new EmailSender());
```

Tomorrow:

```java
NotificationService service =
        new NotificationService(new WhatsAppSender());
```

Yes, this line changes.

However, this is **configuration (object wiring)**, not **business logic**.

The `NotificationService` class remains exactly the same.

---

# What Does "Loose Coupling" Mean?

`NotificationService` depends on:

```java
NotificationSender
```

instead of

```java
EmailSender
```

or

```java
SmsSender
```

or

```java
WhatsAppSender
```

Therefore, any implementation can be injected without modifying the service.

This is called **Programming to an Interface**.

---

# Interview Explanation

> Composition means building objects by combining smaller objects instead of inheriting behavior from a parent class. In this example, `NotificationService` has a `NotificationSender`, which represents a **has-a** relationship. The service contains the common business workflow—validation, logging, and auditing—while the actual sending behavior is delegated to different implementations such as `EmailSender`, `SmsSender`, or `WhatsAppSender`. If a new notification channel is added, I simply create another implementation of `NotificationSender` and inject it into the service. The business logic remains unchanged, making the system easier to extend, maintain, and test.

---

# Design Principles Demonstrated

- Composition (Has-A Relationship)
- Programming to an Interface
- Constructor Dependency Injection
- Runtime Polymorphism
- Open/Closed Principle (OCP)
- Dependency Inversion Principle (DIP)

---

# Key Interview Takeaway

> Composition does **not eliminate changes**.

Instead, it **localizes changes**.

When a new implementation is introduced:

- Add a new implementation class.
- Inject the new implementation.
- Keep the business logic unchanged.

This is the primary reason why production systems prefer **Composition over Inheritance**.