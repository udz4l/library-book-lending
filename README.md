# Library Book Lending System

A small Java console application that demonstrates the Facade design pattern. It reserves a book, creates a loan, and confirms the loan as one workflow.

## Requirements

- Java 17 or newer
- Maven

## Run the application

From the project folder, run:

```text
.\mvnw.cmd clean compile exec:java
```

The program displays a console menu.

## Demo menu

- **1. Complete a normal loan** — runs reservation, lending, and confirmation.
- **2. Demo B failure** — uses an invalid library card. The reservation is cancelled.
- **3. Demo C failure** — simulates confirmation failure. The loan is cancelled first, then the reservation.
- **4. Show saved data** — displays reservations, loans, confirmations, and workflows.
- **5. Recover unfinished workflows** — checks saved workflow states and compensates unfinished work.
- **0. Exit** — closes the program.

## Persistence

The application stores its data in `data/library.json`. The file is created when the application first saves a record. Workflow progress is saved with statuses such as `PENDING_B`, `PENDING_C`, `COMPLETED`, and `COMPENSATED`.

To reset the demo data:

1. Stop the application.
2. Delete `data/library.json`.
3. Run the application again.

## Tests and coverage

Run all tests with:

```text
.\mvnw.cmd test
```

Generate the JaCoCo coverage report with:

```text
.\mvnw.cmd clean verify
```

Open `target/site/jacoco/index.html` to view the report.

The tests cover the normal workflow, both compensation cases, invalid inputs, state transitions, and recovery after reopening the JSON store.

## Main design

- `LibraryMenu` reads user input and calls the facades.
- `LendingFacade` coordinates the reservation, lending, and confirmation services.
- `LibraryAdminFacade` displays saved state and starts recovery.
- Each subsystem has its own entity, service, repository, and status enum.
- `JsonFileStore` reads and writes the JSON file.
- `LendingWorkflow` stores the progress and record IDs for the A → B → C chain.

Subsystems do not call one another. The facades coordinate them.

## Principle of Least Knowledge

Without the facade, the client needs to know about the separate subsystem services and call them in order:

```java
BookReservation reservation =
        reservationService.reserveBook(bookId, readerId);

BookLoan loan =
        lendingService.createLoan(bookId, readerId, true);

LoanConfirmation confirmation =
        confirmationService.createConfirmation(loan.getId(), 14);

confirmationService.confirm(confirmation.getId());
```

With the facade, the client asks for one complete operation:

```java
lendingFacade.lendBook(bookId, readerId, true, false);
```

The facade knows the sequence and handles compensation. The client does not coordinate the three subsystem services.

## Facade and Adapter

An Adapter changes one interface into another interface that a client already expects. It helps two parts communicate when their interfaces do not match.

This project uses a Facade because the client needs one simple operation that coordinates several separate subsystems. The reservation, lending, and confirmation services do not need their interfaces translated. Instead, `LendingFacade` gives the client one entry point, runs the services in order, saves workflow progress, and compensates earlier work when a later step fails.

## Custom exceptions

- `InvalidLibraryCardException` describes a rejected library card.
- `BookUnavailableException` describes a book that already has an active reservation or loan.