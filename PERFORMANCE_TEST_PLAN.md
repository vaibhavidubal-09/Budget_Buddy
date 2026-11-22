# BudgetBuddy transaction performance plan

This is a local, repeatable measurement plan. It does not claim benchmark results until run against the target MySQL machine. H2 test timings are not production performance evidence.

## Setup a disposable synthetic dataset

Use a disposable local MySQL database created for this benchmark, not a development or production database containing project/user data. Create a dedicated test account and replace the email and run tag below. Verify the connection target before running any inserts or deletes.

```sql
SET SESSION cte_max_recursion_depth = 20000;
SET @benchmark_user_id = (SELECT id FROM app_users WHERE email = 'benchmark@example.com');
SET @benchmark_tag = 'BBPERF-OCT26';

INSERT INTO expenses (user_id, description, category, amount, payment_method, date)
WITH RECURSIVE sequence_numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM sequence_numbers WHERE n < 10000
)
SELECT @benchmark_user_id,
       CONCAT(@benchmark_tag, '-', n),
       CASE MOD(n, 6)
           WHEN 0 THEN 'Food & Dining'
           WHEN 1 THEN 'Transportation'
           WHEN 2 THEN 'Shopping'
           WHEN 3 THEN 'Education'
           WHEN 4 THEN 'Entertainment'
           ELSE 'Other'
       END,
       CAST(25 + MOD(n * 37, 4975) AS DECIMAL(12, 2)),
       CASE MOD(n, 3) WHEN 0 THEN 'UPI' WHEN 1 THEN 'Cash' ELSE 'Card' END,
       DATE_SUB(CURDATE(), INTERVAL MOD(n, 365) DAY)
FROM sequence_numbers;
```

Confirm the row count before measuring:

```sql
SELECT COUNT(*) FROM expenses
WHERE user_id = @benchmark_user_id AND description LIKE CONCAT(@benchmark_tag, '-%');
```

Remove only these tagged synthetic rows after the run:

```sql
DELETE FROM expenses
WHERE user_id = @benchmark_user_id AND description LIKE CONCAT(@benchmark_tag, '-%');
```

## Inspect the query plan

Run these on the same MySQL version and dataset. Save the full output with the measured date and MySQL version.

```sql
EXPLAIN ANALYZE
SELECT id, description, category, amount, payment_method, date
FROM expenses
WHERE user_id = @benchmark_user_id
ORDER BY date DESC, id DESC
LIMIT 20;

EXPLAIN ANALYZE
SELECT SUM(amount)
FROM expenses
WHERE user_id = @benchmark_user_id
  AND date BETWEEN DATE_SUB(CURDATE(), INTERVAL 30 DAY) AND CURDATE();
```

The existing `(user_id, date)` expense index is intended for user/date range and sorting access. The page endpoint has an additional `id DESC` tie-breaker; inspect whether MySQL can satisfy the ordering from the index. Add a different index only if `EXPLAIN ANALYZE` and representative filter workloads show a repeatable scan or sort cost that justifies extra write/storage cost. The contains-text search (`LIKE '%term%'`) is not expected to use a normal B-tree index.

## Measure HTTP retrieval

Start the backend and Vite or the deployed local build. Set credentials for the disposable test account in the current PowerShell session (do not put them in a committed file):

```powershell
$env:BUDGETBUDDY_BENCH_EMAIL = 'benchmark@example.com'
$env:BUDGETBUDDY_BENCH_PASSWORD = 'your-local-test-password'
python tools/performance/benchmark_transactions.py --requests 100 --concurrency 10
```

The script warms each route, then compares `GET /api/expenses` with `GET /api/expenses/page?page=0&size=20`. It reports response row count, success/error rate, throughput, mean latency, p50 and p95 latency. Run at concurrency 1, 10, and 25; repeat each scenario three times and report the median of each run’s p95. Keep the data, machine specs, JDK, MySQL version, and exact command with the report. The full-list route intentionally transfers every matching record and is included as a comparison baseline.

For a write-concurrency measurement, add a separate client scenario that sends simultaneous expense requests with unique idempotency keys, and another that sends identical same-key retries. Report successful response count, conflict/error count, duplicate row count, throughput and p95. Never infer production capacity from a single laptop run.

## Current evidence and limits

- API integration and concurrent retry behavior are exercised with H2 in MySQL compatibility mode. A MySQL Testcontainers suite also exists for migrations, uniqueness, and concurrent budget updates, but its three tests were skipped in the latest run because Docker was unavailable. No MySQL-specific behavior was verified in that run.
- The latest full backend test run on 2026-10-18 completed with 66 tests, 0 failures, 0 errors, and 3 skipped.
- No MySQL `EXPLAIN ANALYZE`, real load run, latency result, p95, throughput, or capacity estimate has been captured yet.
- `GET /api/expenses/page` is available, but some dashboard/budget consumers still request the full expense list. Migrate those consumers before removing the baseline endpoint.
- Summary expense totals and category totals use database `SUM` and `GROUP BY`; recurring income is still expanded from the user's income entries in Java.
