# BudgetBuddy Interview Guide

This guide describes implemented code and the latest recorded tests. It separates verified application behavior from behavior that still needs MySQL or live-provider verification.

## Project summary

BudgetBuddy is a React and Spring Boot personal-finance application. Users can record expenses and income, manage weekly/monthly/yearly category budgets, view analytics, and optionally ask Gemini to interpret transaction text or explain financial summaries. The project demonstrates a layered REST API, user-scoped data access, database migrations, validation, idempotent writes, and transaction/concurrency handling.

## Architecture and request flow

**Stack:** React 19, Vite, Java 17 source release, Spring Boot 4.1.1, Spring MVC, Spring Security OAuth2 Resource Server, Bean Validation, Spring Data JPA/Hibernate, Flyway, and MySQL 8.4. Maven tests use H2 and MySQL Testcontainers (the latter requires Docker).

A frontend API helper sends JSON and, for protected routes, a bearer JWT. Spring Security verifies the token before the controller is called. The controller validates the request and extracts the owner ID from the JWT. A service applies business rules and transaction boundaries; repositories execute owner-filtered JPA queries or SQL aggregates; entities map rows in MySQL.

**Relevant code:** `src/services/*ApiService.js`; `backend/src/main/java/com/budgetbuddy/api/SecurityConfig.java`; controller, service, repository, and entity packages below `backend/src/main/java/com/budgetbuddy/api/`.

## 1. Authentication, authorization, and password storage

Registration and login are public; all other routes require authentication except `OPTIONS` and the health route. `AuthService` normalizes email, hashes passwords with BCrypt, and returns a generic invalid-credentials response. `JwtTokenService` issues HS256 tokens with issuer `budgetbuddy`, user ID, email subject, name, and a 12-hour expiration. `SecurityConfig` verifies signature, issuer, and time claims. Its Base64 secret must decode to at least 32 bytes and is supplied through configuration, not hardcoded.

For individual expense, income, and budget records, service lookups use both the record ID and authenticated user ID. Controllers do not accept an owner ID from the client for these operations. Cross-user API integration tests verify GET/PUT/DELETE return not found and do not change the owner's row.

**Relevant code:** `auth/AuthService.java`, `auth/JwtTokenService.java`, `auth/JwtUserId.java`, `SecurityConfig.java`, `expenses/ExpenseService.java`, `income/IncomeService.java`, `budgets/BudgetService.java`.

**Interview question:** How does the backend prevent an ID from another account being used to access a record?

**Answer:** The controller gets the user ID from the authenticated JWT. The service repository lookup includes both the path ID and that user ID, such as `findByIdAndUserId`. A foreign record is therefore treated as not found. The integration tests exercise this for expenses, income, and budgets.

**Limitations:** Login/registration have no application-level rate limiter. Add suitable edge rate limits before public exposure. JWTs live for 12 hours and there is no refresh/revocation mechanism. Production HTTPS, deployment CORS, and managed secret storage still need deployment-specific setup.

## 2. Validation, money, and API errors

Expense and income amounts use Java `BigDecimal`, mapped to MySQL `DECIMAL(12,2)`. Their request DTOs require positive values and enforce at most 10 integer digits and 2 fractional digits using Bean Validation. Controllers use `@Valid`; the expense import batch validates each nested row too. Budget limits are positive whole-number integers. Dates are checked for null and MySQL's supported `DATE` range; enum-like income frequencies and budget periods are constrained.

`ApiExceptionHandler` maps validation, malformed JSON/date, request parameter, conflict, and status exceptions to `ApiError`. Unexpected errors use a generic message instead of exposing stack traces or SQL details. `AuthResponse.toString()` redacts token and personal details because Spring DEBUG response logging can format response objects; Jackson JSON still contains the required token field.

**Relevant code:** request DTOs in `expenses/`, `income/`, `budgets/`, `auth/`; `validation/MySqlDate.java`; `ApiExceptionHandler.java`; `auth/AuthResponse.java`.

**Interview question:** Why use `BigDecimal` and validate scale at the HTTP boundary?

**Answer:** Binary floating-point cannot exactly represent many decimal currency values. `BigDecimal` matches the DECIMAL column. `@Positive` and `@Digits(integer=10,fraction=2)` reject invalid values before JPA/database conversion, giving clients a 400 instead of a persistence error. Integration tests cover normal values, maximum value, excessive scale/range, zero, and negative amounts for expense and income.

## 3. Database design and Flyway

The schema has four core tables:

- `app_users`: ID, unique email, full name, BCrypt password hash.
- `expenses`: owner, description, category, `DECIMAL(12,2)` amount, payment method, date, optional split text, import fingerprint, and idempotency key/payload hash.
- `income_entries`: owner, source, `DECIMAL(12,2)` amount, start date, recurrence frequency, optional interval days.
- `category_budgets`: owner, category, whole-number limit, period, and optimistic version.

Flyway history:

| Version | Change |
| --- | --- |
| V1 | Core tables plus user/date and budget lookup indexes. |
| V2 | User foreign keys with delete restriction. |
| V3 | Ensure expected query indexes exist (Java migration). |
| V4 | Expense import fingerprint and per-user unique index. |
| V5 | Budget `version` column. |
| V6 | Expense idempotency key/hash and per-user key uniqueness. |
| V7 | Unique `(user_id, period, category)` budget constraint. |

Hibernate uses `ddl-auto=validate`; migrations, rather than Hibernate, own schema changes. V1–V7 successfully run in the H2 tests. The Testcontainers test intended to run all migrations against MySQL was skipped without Docker, so MySQL compatibility is not established by the latest test run. Existing databases must not be baselined from table names alone: verify the full schema against the intended baseline first. Ownerless rows are not automatically assigned to a new account.

**Relevant code:** `backend/src/main/resources/db/migration/V1` through `V7`; `backend/src/main/java/db/migration/V3__ensure_query_indexes.java`; `backend/src/main/resources/application.properties`.

## 4. Query behavior and analytics periods

Expense pages use `ExpenseRepository.findPageByUser` with pagination, owner filter, optional filters, and date/ID descending order. The page size is limited to 100. Analytics expense totals and date/category totals use database `SUM` and `GROUP BY` queries, bounded by date windows. The assistant context uses the same aggregate query over the previous-year start through the current-year end instead of loading every expense. Income recurrence is different: SQL fetches schedules relevant to the period, then Java counts scheduled occurrences.

`FinancialPeriodCalculator` uses an injected `Clock` configured for `Asia/Kolkata`. Weekly windows are Monday through Sunday; monthly and yearly windows use calendar boundaries. Analytics and assistant context share the calculator. Tests cover week boundaries and month/year transitions.

One scale limitation remains: the Expenses page is paginated, but `App.jsx` loads the full expense collection on startup and after expense changes. The legacy `GET /api/expenses` endpoint also remains. Do not claim every UI path is paginated.

**Relevant code:** `summary/SummaryService.java`, `assistant/AssistantFinancialContextService.java`, `expenses/ExpenseRepository.java`, `income/IncomeRepository.java`, `time/FinancialPeriodCalculator.java`, `time/FinancialTimeConfiguration.java`, `src/pages/ExpensesPage.jsx`, `src/App.jsx`.

## 5. Transactions, concurrency, and idempotency

### Budget writes

`Budget` has JPA `@Version`. `BudgetService` write methods run transactionally and first lock the owning user row through `UserAccountRepository.findByIdForUpdate` (`PESSIMISTIC_WRITE`). This serializes writes for one owner; the version in an update request or full-plan expected snapshot detects stale client state and produces 409. The V7 unique constraint is the final duplicate-prevention guard. `applyPlan` validates the entire request, replaces only one period in one transaction, preserves other periods, and rolls back changes on a failure. Empty plans clear the selected period.

This combines a per-owner pessimistic lock with entity optimistic version checks. It is intentionally simple and user-scoped; the owner row is the serialization point. The H2 integration suite tests duplicate creation, concurrent update behavior, stale versions, ownership, plan rollback, and period preservation. MySQL-specific constraint and concurrent update tests exist but were skipped because Docker was unavailable. Therefore do not claim the locking behavior has been validated against MySQL.

### Expense retries and imports

A caller can pass `Idempotency-Key` when creating an expense. The backend stores the key and a hash of the request payload. A repeated key with the same payload returns the prior expense; a different payload with that key returns 409. The owner row is locked while checking/creating and a per-user unique database index protects the key. The frontend creates a key for new expenses and reuses it when retrying the same pending object.

Imports are previewed first. A stable source transaction ID is preferred for the fingerprint; otherwise date, amount, normalized description, and payment method are used. A `HashSet` tracks duplicates within a batch. Without a bank-provided ID, similar legitimate purchases can be false positives, so the user reviews the preview.

**Relevant code:** `budgets/BudgetService.java`, `budgets/Budget.java`, `auth/UserAccountRepository.java`, `expenses/ExpenseService.java`, migrations V4–V7.

**Interview question:** Why is `@Transactional` alone not enough for a budget update?

**Answer:** A transaction groups database operations, but it does not by itself prevent two requests from making decisions using the same stale state. The service locks the owner's row to serialize that owner's writes, compares the client's version/snapshot, and relies on `@Version` and unique indexes as additional consistency checks. Database-specific concurrency tests are still needed; current MySQL Testcontainers checks did not run.

## 6. Gemini integration and failure handling

Gemini is called from backend services through the shared `GeminiApiClient`, using a configured backend-only API key. The HTTP client has a 3-second connect timeout and a 15-second read timeout. It makes at most two attempts, retrying HTTP 429, HTTP 5xx, and access failures once after 200 ms; ordinary client errors are not retried. AI output is parsed and validated before it is presented or used. Manual expense tracking remains available when AI is unavailable.

`GeminiApiClientTest` uses mocked HTTP responses for retry, non-retryable status, and malformed model output. Tests do not contact Google. The local environment has reported HTTP 503 from Gemini-backed features, so live credentials, quota, current model availability, and provider success remain unverified.

**Relevant code:** `expenses/GeminiClientConfiguration.java`, `expenses/GeminiApiClient.java`, `expenses/ExpenseUnderstandingService.java`, `budgets/BudgetPlanSuggestionService.java`, `assistant/AssistantChatService.java`.

## 7. Tests and actual evidence

Run from `backend`:

```powershell
mvn test
```

The latest recorded run on 2026-10-18 completed with **66 tests, 0 failures, 0 errors, 3 skipped**. The skipped tests are the three `BudgetMySqlIntegrationTest` methods because Docker was not available. H2 API integration tests exercise ownership, validation, rollback, analytics boundaries, import deduplication, and budget/idempotency concurrency flows. Unit tests mock Gemini and other isolated dependencies. H2 tests are not proof of MySQL behavior.

The frontend build command is `npm ci` followed by `npm run build` from the repository root. GitHub Actions is configured to run that build and `mvn -B test`; this guide does not claim that a remote CI run occurred.

## 8. Performance and system design trade-offs

The project is a modular monolith. It uses indexes aligned with owner/date and owner/period/category lookups, database aggregation for expense summaries, and pagination for the expense browsing page. `HashSet` fingerprint membership and `HashMap` category lookup are expected O(1) per operation; those structures use O(B) and O(C) memory for batch rows and categories. Page transfer is O(P) for page size P; the remaining full-list frontend fetch is O(N) for N stored expenses. A Java top-K `PriorityQueue` was not added because current use cases can be served by database ordering and limits.

`PERFORMANCE_TEST_PLAN.md` describes synthetic dataset creation, `EXPLAIN ANALYZE`, and HTTP measurements. No real MySQL latency, p95, throughput, capacity benchmark, or production result has been recorded. The next engineering step for reliable capacity claims is to run the plan against a disposable MySQL environment and record machine/database details alongside results.

## Five interview questions to practice

1. **How do you ensure one user cannot access another user's finance row?** JWT-derived owner ID is included in service/repository lookups; integration tests check foreign-owner reads and writes.
2. **How are simultaneous budget edits handled?** An owner-row pessimistic lock serializes writes; request versions and JPA `@Version` catch stale state; V7 uniqueness protects the budget key. MySQL test verification is pending Docker availability.
3. **How does expense idempotency work?** A persisted per-user key and payload hash distinguish retries from conflicting reuse, with a unique index as the database guard.
4. **How are weekly totals defined?** A shared Asia/Kolkata `Clock` and `FinancialPeriodCalculator` define weeks Monday–Sunday and calendar month/year boundaries for both analytics and assistant data.
5. **What is the current scale limit you can defend?** No measured capacity claim. The expense page is paginated and summaries aggregate in SQL, but the app still fetches the full expense collection for initial state and refreshes. I would benchmark with the documented MySQL test plan before estimating capacity.

## Implemented versus future work

**Implemented:** auth and owner-scoped CRUD; request validation; V1–V7 migrations; SQL expense aggregation; date-consistent analytics and assistant context; budget optimistic version checks, owner-row serialization, uniqueness, and atomic plan application; expense idempotency and import deduplication; mocked Gemini failure tests; safe auth response stringification.

**Still to verify or improve:** run MySQL Testcontainers tests with Docker; verify migration behavior against a disposable MySQL instance; replace the remaining full-list frontend expense loads; add edge rate limits; verify uploaded file signatures; configure production HTTPS, secrets, CORS, backups, and monitoring; resolve live Gemini 503/quota behavior; collect actual performance measurements.
