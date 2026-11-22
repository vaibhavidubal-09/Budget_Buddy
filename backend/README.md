# BudgetBuddy Spring Boot API

This directory contains the Java backend for the React/Vite BudgetBuddy application. It provides REST APIs for authentication, expenses, income, budgets, analytics, and optional Gemini-assisted features.

## Stack and request flow

Java 17 source compatibility, Spring Boot 4.1.1, Spring MVC, Spring Security OAuth2 Resource Server, Bean Validation, Spring Data JPA/Hibernate, Flyway, and MySQL 8.4. Controllers validate and route HTTP requests, services apply business rules and transaction boundaries, repositories query persistence, and JPA entities map rows. Protected finance requests derive the owner from the verified JWT.

## Local startup

Requirements: Java 17 or newer, Maven 3.9+, and MySQL 8+.

1. Create a new, empty local `budgetbuddy` database with `database/create-database.sql`.
2. Set these environment variables in your IDE run configuration or terminal:
   - `BUDGETBUDDY_DB_URL` (optional; default `jdbc:mysql://localhost:3306/budgetbuddy`)
   - `BUDGETBUDDY_DB_USERNAME` (optional; default `root`)
   - `BUDGETBUDDY_DB_PASSWORD` (required)
   - `BUDGETBUDDY_JWT_SECRET` (required; Base64 value decoding to at least 32 random bytes)
   - `GEMINI_API_KEY` (optional; blank disables provider-backed AI)
   - `GEMINI_MODEL` (optional; defaults to `gemini-3.8-flash`)
   - `BUDGETBUDDY_CORS_ALLOWED_ORIGINS` (optional; defaults to `http://localhost:5173`)
3. From this directory, run `mvn spring-boot:run`, or start `BudgetBuddyApiApplication` in IntelliJ.

Flyway applies migrations on startup. `spring.jpa.hibernate.ddl-auto=validate` makes Hibernate check the mapped schema without creating or altering tables. Do not point the application at a database containing user data until its schema/history and migration plan have been verified.

Generate a signing key in Windows PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Keep secrets outside tracked files. `.env.example` contains placeholders only. The application requires the same signing key between restarts to keep existing tokens valid; rotation invalidates those tokens.

### Existing non-empty database warning

The application defaults `BUDGETBUDDY_FLYWAY_BASELINE_ON_MIGRATE` to `false`, with baseline version `1`. Only consider baselining a non-empty schema with no Flyway history after a backup and a full comparison showing that the existing tables exactly match V1. Flyway would record V1 as already applied and then run V2 through V7. If any V2–V7 changes already exist, or the schema differs from V1, stop and reconcile the schema deliberately. Do not enable baseline globally or manually edit Flyway history. Orphaned non-null user references can prevent the foreign-key migration. Rows with `user_id IS NULL` stay unassigned; registration does not claim them.

## API route groups

All routes require a valid bearer token except `GET /api/health`, `POST /api/auth/register`, and `POST /api/auth/login`. CORS defaults to the local Vite origin and is configurable.

### Authentication

- `POST /api/auth/register` - create an account; stores a BCrypt hash and returns a token.
- `POST /api/auth/login` - verify credentials and return a token.
- `GET /api/auth/me` - return the account represented by the token.

JWTs are HS256, have issuer `budgetbuddy`, include a user ID claim, and expire after 12 hours. The decoder validates the signature, issuer, and token time claims. The signing secret is Base64-decoded and must contain at least 32 bytes.

### Expenses and imports

- `GET /api/expenses` - all expenses for the signed-in user, date descending.
- `GET /api/expenses/page?page=0&size=20` - paginated/filterable list; supports `category`, `search`, `startDate`, and `endDate`. Page size is 1 to 100; order is date then ID descending.
- `GET`, `PUT`, `DELETE /api/expenses/{id}` - owner-scoped record access.
- `POST /api/expenses` - create with optional `Idempotency-Key` header.
- `POST /api/expenses/import/preview`, `POST /api/expenses/import` - validate, preview, and save a transaction batch with fingerprint duplicate handling.
- `POST /api/expenses/understand`, `POST /api/expenses/suggest-category` - optional AI entry and category suggestions.
- `POST /api/expenses/import-image` or `/api/expenses/import-file` - extract a reviewable transaction preview from an uploaded PDF/image. CSV parsing happens in the frontend; the backend accepts normalized transaction batches.

An amount must be positive and fit `DECIMAL(12,2)` (`@Digits(integer=10, fraction=2)`). Expense and income requests use `BigDecimal`. Validation errors use the common `ApiError` response. Unexpected errors return a generic message rather than internal exception details.

### Income

- `GET /api/income` - list the signed-in user's entries.
- `GET`, `PUT`, `DELETE /api/income/{id}` - owner-scoped record access.
- `POST /api/income` - create an income entry.

Frequency is `ONCE`, `WEEKLY`, `MONTHLY`, `YEARLY`, or `CUSTOM` with an interval in days. Scheduled income used in summaries is an estimate, not confirmed receipts.

### Budgets, analytics, and assistant

- `GET /api/budgets`, `GET /api/budgets/{id}`, `POST /api/budgets`, `PUT /api/budgets/{id}`, `DELETE /api/budgets/{id}` - owner-scoped category budgets.
- `POST /api/budgets/apply-plan` - atomically replace a complete budget set for one period. It checks the expected budget IDs and versions. Omitted budgets in the selected period are deleted; other periods are preserved. An empty list clears only the selected period.
- `POST /api/budgets/suggest-plan`, `POST /api/budgets/suggest-category` - optional Gemini suggestions; applying a plan is a separate authenticated request.
- `GET /api/summary/monthly` and `GET /api/summary/period?period=weekly|monthly|yearly` - owner-scoped financial summaries and budget usage.
- `POST /api/assistant/chat` - authenticated assistant. The backend constructs the financial context from persisted records for the token owner before calling Gemini.

Financial periods use `Asia/Kolkata`; weeks are Monday through Sunday. Summary and assistant context share `FinancialPeriodCalculator`. Expense summary totals and category totals use database aggregates. Relevant income schedule rows are selected in SQL and recurrence occurrences are counted in Java.

## Schema history

| Version | Purpose |
| --- | --- |
| V1 | Create `app_users`, `expenses`, `income_entries`, and `category_budgets`; initial lookup indexes. |
| V2 | Add foreign keys from finance tables to `app_users`, with delete restriction. |
| V3 | Ensure user/date and user/period/category indexes. |
| V4 | Add expense import fingerprint and per-user uniqueness. |
| V5 | Add budget optimistic-lock version. |
| V6 | Add expense idempotency key and payload hash, unique per user/key. |
| V7 | Add unique budget constraint for user, period, and category. |

Migrations live in `src/main/resources/db/migration/`, except V3, which is a Java migration under `src/main/java/db/migration/`. Never edit a migration already applied to a database; add a new version for future schema changes.

## Concurrency and transaction notes

`Budget` has `@Version`. `BudgetService` write methods use a transaction and lock the owning `app_users` row with `PESSIMISTIC_WRITE`; this serializes writes for a single owner. Update requests compare the supplied version and stale writes return 409. The unique constraint is the cross-request/database safeguard for duplicate budget keys. `applyPlan` validates its snapshot, deletes omitted selected-period rows, saves replacements, and returns the result within one transaction; an exception rolls back the transaction.

Expense idempotency uses a persistent per-user key and payload hash. Same-key retries with the same payload replay the stored row; conflicting payloads return 409. Import batches fingerprint rows and use a database unique index. Regular behavioral integration tests exercise concurrency and rollback using H2, not MySQL.

## Tests and verification

From this directory:

```powershell
mvn test
```

Latest recorded full suite: **66 tests, 0 failures, 0 errors, 3 skipped** on 2026-10-18. `BudgetMySqlIntegrationTest` has three MySQL Testcontainers tests for clean migrations, the unique constraint, and simultaneous budget updates. All three skipped because Docker was unavailable. Therefore MySQL migration/locking behavior is not verified by that run. Other API integration tests use an isolated in-memory H2 database, and unit tests mock dependencies. Gemini HTTP behavior is mocked; no live Gemini request is made during the suite.

For the frontend build, run `npm ci` and `npm run build` from the repository root. See the root README for current verification and known limitations.
