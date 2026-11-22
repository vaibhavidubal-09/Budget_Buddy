# BudgetBuddy

BudgetBuddy is a personal-finance tracker for recording expenses and income, setting category budgets, and understanding spending over calendar periods. It keeps financial records scoped to the signed-in account and offers optional Gemini-assisted entry and explanations. The AI features are helpers: manual transaction entry does not depend on AI.

## Project at a glance

- **Frontend:** React 19, React Router, Vite, JavaScript.
- **Backend:** Java (Maven compiler release 17), Spring Boot 4.1.1, Spring MVC, Spring Security, Bean Validation, Spring Data JPA/Hibernate.
- **Database:** MySQL 8.4 in Docker Compose; Flyway migrations manage schema changes. H2 is used by the regular API integration tests.
- **Authentication:** stateless HS256 JWT bearer tokens; BCrypt password hashes.
- **AI provider:** Google Gemini, called server-side. No Gemini key is needed for manual tracking or mocked-provider tests.

## Architecture and request flow

```text
React + Vite (src/)
  -- JSON over HTTP; bearer JWT for protected routes -->
Spring Boot controllers
  --> request validation and authenticated user ID
Spring services
  --> business rules and transaction boundaries
Spring Data JPA repositories / SQL aggregates
  -->
MySQL
```

The React API helpers attach the session token. Controllers take the user ID from the validated JWT rather than trusting a user ID in request JSON. Services apply business rules and use repositories for persistence. Finance rows are queried by both record ID and owner ID for individual reads and changes. Gemini credentials remain on the backend.

## Implemented features

- Registration, login, and current-user endpoint. Passwords are BCrypt-hashed. Password and email requests are validated. JWTs use the `budgetbuddy` issuer, expire after 12 hours, and require a Base64 signing secret that decodes to at least 32 bytes.
- Expense and income CRUD, with date, category, schedule, and amount validation. Expense amounts use `BigDecimal` and must fit MySQL `DECIMAL(12,2)`: positive, up to 10 integer digits and 2 fractional digits. Income recurrence supports once, weekly, monthly, yearly, and custom day intervals.
- Expense browsing through `GET /api/expenses/page` with server-side pagination, category/search/date filters, stable date/ID sorting, and a maximum page size of 100. The older `GET /api/expenses` route still returns the signed-in user's complete list; `App.jsx` uses it for initial state and refresh after expense changes. The Expenses page itself uses the paginated route.
- CSV import is parsed in the browser, previewed, then submitted as a validated batch. PDF and image statement extraction returns a reviewable AI preview. Import fingerprints help identify repeated rows; similar real purchases can still be flagged when a source transaction ID is absent.
- Optional expense understanding, category suggestions, budget-plan suggestions, document extraction, and assistant chat via Gemini. Users review suggested transaction details before saving. The assistant financial context is calculated by the backend from the signed-in user's data; recurring income figures are estimates based on saved schedules.
- Weekly, monthly, and yearly analytics with previous-period comparisons, category spending, trends, and budget usage. Expense totals are calculated with database aggregation queries. Recurring income schedules are selected from the relevant date range and their occurrences are expanded in Java.
- Weekly, monthly, and yearly periods use `Asia/Kolkata`; weeks run Monday through Sunday. Summary and assistant context share `FinancialPeriodCalculator` and the configured application `Clock`.
- Bill splitting calculates shares and can hand off a payment to a UPI app. BudgetBuddy does not initiate, confirm, or track payment completion.

## Data integrity and concurrency

Flyway V1–V7 create the schema and add ownership, indexes, import deduplication, budget versions, expense idempotency, and unique budget ownership/category/period constraints. Hibernate uses `ddl-auto=validate`; it checks mappings without creating or altering the schema.

Budget rows have a JPA `@Version`. Budget write operations also lock the owning user row with a pessimistic write lock, serializing budget changes for the same user. Update requests carry a version, and stale versions return HTTP 409. Applying a complete AI-suggested plan uses one transactional backend request: it replaces budgets only for the selected period, preserves other periods, and checks the expected ID/version snapshot. Database uniqueness remains the final guard against duplicates.

Expense creation accepts an optional `Idempotency-Key` (the frontend sends one). For a same-key/same-payload retry, the API returns the original expense; a different payload with the key returns 409. Concurrent same-key requests are serialized by an owner-row lock and protected by a database unique index. Batch imports use per-user fingerprints and database uniqueness.

## Database migrations

| Version | Change |
| --- | --- |
| V1 | Create `app_users`, `expenses`, `income_entries`, `category_budgets`; add baseline user/date and budget lookup indexes. |
| V2 | Add user foreign keys with delete restriction. |
| V3 | Ensure the expected user/date and user/period/category indexes exist. |
| V4 | Add expense import fingerprints and their per-user unique index. |
| V5 | Add the budget optimistic-lock `version` column. |
| V6 | Add expense idempotency key/payload hash and per-user unique index. |
| V7 | Enforce one budget per user, period, and category. |

For a new local database, create an empty `budgetbuddy` schema and let Flyway apply V1–V7. For an existing non-empty database without Flyway history, do not enable baseline merely because the table names look familiar. Back up the database and compare the full schema with the intended baseline. The configured baseline is version 1: it marks V1 as already applied and then executes V2 onward. If the existing schema contains changes from later versions or differs from V1, stop and reconcile deliberately; do not baseline or migrate data blindly. Ownerless legacy rows remain unassigned, and new accounts do not claim them.

## Run locally

Requirements: Node.js 22 (also used by CI), Java 17 or newer, Maven 3.9+, and MySQL 8+.

1. Create an empty local schema using `backend/database/create-database.sql` or create it in MySQL Workbench.
2. Configure backend environment variables in the IDE run configuration or terminal; never put real values in source files:
   - `BUDGETBUDDY_DB_URL` (optional; defaults to `jdbc:mysql://localhost:3306/budgetbuddy`)
   - `BUDGETBUDDY_DB_USERNAME` (defaults to `root`)
   - `BUDGETBUDDY_DB_PASSWORD` (required)
   - `BUDGETBUDDY_JWT_SECRET` (required; Base64 representing at least 32 random bytes)
   - `GEMINI_API_KEY` (optional; enables Gemini calls)
   - `GEMINI_MODEL` (optional; default `gemini-3.8-flash`)
   - `BUDGETBUDDY_CORS_ALLOWED_ORIGINS` (optional; default `http://localhost:5173`)
3. Start the backend from `backend` with `mvn spring-boot:run`, or run `BudgetBuddyApiApplication` from IntelliJ.
4. From the project root, run `npm ci` and `npm run dev`.
5. Open `http://localhost:5173`. The health endpoint is `http://localhost:8080/api/health`.

Generate a JWT secret in Windows PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Keep the secret private and stable between backend restarts if existing sessions should remain valid. Rotating it invalidates existing tokens. Never copy a real API key, database password, or JWT secret into `.env.example`, documentation, or source control.

### Docker Compose

Copy `.env.example` to `.env`, replace the local database passwords and JWT placeholder, and optionally set `GEMINI_API_KEY`. Then run from the project root:

```powershell
docker compose up --build
```

The frontend is served at `http://localhost:5173`, the API at `http://localhost:8080`. Compose stores MySQL data in the named `budgetbuddy-db` volume. Do not remove that volume if you need to preserve its contents. This is a local setup, not evidence of a production deployment.

## Tests and current verification

Run the frontend production build from the project root:

```powershell
npm ci
npm run build
```

Run backend tests from `backend`:

```powershell
mvn test
```

Latest recorded backend run: **66 tests, 0 failures, 0 errors, 3 skipped**, on 2026-10-18. The skipped tests are `BudgetMySqlIntegrationTest` cases requiring Docker/MySQL Testcontainers; Docker was unavailable, so those tests did not verify MySQL. The rest of the suite uses unit tests and isolated H2 integration tests that apply migrations V1–V7. Do not interpret H2 compatibility mode as proof of MySQL locking or migration behavior.

The frontend production build passed on 2026-10-18 with Vite 6.4.3 (`npm.cmd run build` in PowerShell; `npm run build` works in shells where the npm script is not blocked by execution policy).

Gemini client behavior is tested with mocked HTTP responses for rate limiting, non-retryable errors, and malformed output. There are no live Gemini tests. Gemini-backed features have returned HTTP 503 in the reported local session; live quota, credentials, and provider availability remain unverified. Manual tracking is independent of Gemini.

GitHub Actions is configured to run `npm ci`, `npm run build`, and `mvn -B test`. No production deployment, MySQL benchmark, or measured capacity claim is documented.

## Known limitations and next work

- Login and registration do not have application-level rate limiting. Configure appropriate limits at the deployment edge before exposing them publicly.
- Upload limits are 10 MB per file and 11 MB per request. The server checks the declared MIME type but does not verify the file signature.
- The frontend's initial expense state and post-change refresh still use the full-list endpoint even though the Expenses page is paginated.
- MySQL-specific Testcontainers tests were skipped because Docker was unavailable; MySQL behavior and this project's live schema have not been verified in this test run.
- Gemini calls have mock-based tests only, and live provider requests currently return 503 in the reported local environment.
- Docker Compose is for local development. Production HTTPS, deployment secrets, backups, monitoring, and deployment-specific CORS still need setup and verification.
- `PERFORMANCE_TEST_PLAN.md` describes measurements to run. No MySQL `EXPLAIN ANALYZE`, latency, p95, throughput, or user-capacity results have been recorded.

See [backend/README.md](backend/README.md) for API routes and backend-specific setup, and [INTERVIEW_PREPARATION.md](INTERVIEW_PREPARATION.md) for code-based interview explanations.
