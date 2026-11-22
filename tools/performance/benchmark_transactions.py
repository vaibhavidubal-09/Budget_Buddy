"""Small dependency-free local benchmark for BudgetBuddy expense retrieval APIs."""

import argparse
import concurrent.futures
import json
import os
import statistics
import time
import urllib.error
import urllib.request


def request_json(url, method="GET", body=None, token=None):
    headers = {"Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    payload = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=payload, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=60) as response:
        data = response.read()
        return response.status, json.loads(data) if data else None


def percentile(values, percent):
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, int((percent / 100) * len(ordered) + 0.999) - 1))
    return ordered[index]


def run_case(label, url, token, count, concurrency):
    def one_request(_):
        started = time.perf_counter()
        try:
            status, result = request_json(url, token=token)
            ok = 200 <= status < 300
            row_count = len(result) if isinstance(result, list) else len(result.get("items", []))
            return (time.perf_counter() - started) * 1000, ok, row_count
        except (urllib.error.URLError, TimeoutError, ValueError):
            return (time.perf_counter() - started) * 1000, False, 0

    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as pool:
        results = list(pool.map(one_request, range(count)))
    elapsed = time.perf_counter() - started
    latencies = [result[0] for result in results]
    successes = sum(1 for result in results if result[1])
    failures = count - successes
    rows_returned = results[0][2] if results and successes else 0
    print(f"{label}: requests={count} concurrency={concurrency} rows_per_response={rows_returned}")
    print(f"  success={successes} errors={failures} error_rate={failures / count:.2%} throughput={successes / elapsed:.2f} req/s")
    print(f"  latency_ms mean={statistics.fmean(latencies):.2f} p50={percentile(latencies, 50):.2f} p95={percentile(latencies, 95):.2f} max={max(latencies):.2f}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:8080/api")
    parser.add_argument("--requests", type=int, default=100)
    parser.add_argument("--concurrency", type=int, default=10)
    args = parser.parse_args()
    if args.requests < 1 or args.concurrency < 1:
        parser.error("--requests and --concurrency must be positive")

    email = os.environ.get("BUDGETBUDDY_BENCH_EMAIL")
    password = os.environ.get("BUDGETBUDDY_BENCH_PASSWORD")
    if not email or not password:
        parser.error("Set BUDGETBUDDY_BENCH_EMAIL and BUDGETBUDDY_BENCH_PASSWORD in your shell")

    status, login = request_json(args.base_url.rstrip("/") + "/auth/login", "POST",
                                 {"email": email, "password": password})
    if status != 200 or not login.get("token"):
        raise SystemExit("Login failed; benchmark was not run")
    token = login["token"]
    base = args.base_url.rstrip("/")
    # Warm each route a few times so startup/JIT costs do not dominate the sample.
    request_json(base + "/expenses", token=token)
    request_json(base + "/expenses/page?page=0&size=20", token=token)
    run_case("full-list", base + "/expenses", token, args.requests, args.concurrency)
    run_case("page-20", base + "/expenses/page?page=0&size=20", token, args.requests, args.concurrency)


if __name__ == "__main__":
    main()
