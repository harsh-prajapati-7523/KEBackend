# 100k Ticket Performance Test Plan

This kit is for measuring Kumar Electronics ticket performance with production-like data volume.
Run it only against a disposable local database or a dedicated test environment.

## Goals

- Validate API and browser behavior with 100,000 tickets.
- Find slow queries before users feel them.
- Confirm pagination prevents large frontend renders.
- Confirm Create Ticket customer lookup stays fast with repeated mobile numbers.

## Success Targets

| Area | Target |
| --- | --- |
| `POST /volt/tickets` | p95 under 1s |
| `GET /volt/tickets?page=0&size=20` | p95 under 1s |
| `GET /volt/tickets/my?page=0&size=20` | p95 under 1s |
| `GET /volt/tickets/search?query=<mobile>` | p95 under 1.5s |
| `GET /volt/tickets/query?...` | p95 under 1.5s |
| `GET /volt/tickets/{id}` | p95 under 1s |
| `GET /volt/tickets/{id}/customer-history` | p95 under 1s |
| `GET /volt/tickets/customer-lookup?mobileNumber=<number>` | p95 under 500ms |
| Browser list/detail screens | no visible freeze on mobile viewport |

## Data Shape

The seed script creates realistic fixed ticket data:

- 100k tickets by default.
- Repeated mobile numbers across many tickets.
- Mixed customer names, villages, product types, statuses, owners, completion/cancellation states.
- Dates spread across roughly two years.
- Ticket numbers with `KE-PERF-` prefix so they are easy to identify and optionally delete.

## Files

- `seed-100k-tickets.sh`: wrapper around `psql`.
- `seed-100k-tickets.sql`: bulk ticket seed data.
- `suggested-ticket-indexes.sql`: candidate indexes to test after baseline.
- `api-performance-check.mjs`: API benchmark runner using Node fetch.
- `../KEOnline/qa-artifacts/performance-100k/browser-performance-check.mjs`: browser timing runner using Playwright, relative to the project root.

## Step 1: Prepare Test Environment

1. Point backend to a disposable PostgreSQL database.
2. Start backend normally and let Hibernate create/update tables.
3. Confirm login works with a SUPER_ADMIN or equivalent user.
4. Make sure the environment is not production.

## Step 2: Baseline Without New Indexes

Seed data:

```bash
cd KEBackend
DATABASE_URL='postgresql://user:password@host:5432/dbname' \
  tools/performance/seed-100k-tickets.sh
```

If you need to replace previous performance tickets:

```bash
DATABASE_URL='postgresql://user:password@host:5432/dbname' \
  DELETE_EXISTING=1 \
  TICKET_COUNT=100000 \
  tools/performance/seed-100k-tickets.sh
```

Run API benchmark:

```bash
cd KEBackend
BASE_URL='http://127.0.0.1:9001' \
EMPLOYEE_ID='SUPER_ADMIN_001' \
PASSWORD='admin123' \
  node tools/performance/api-performance-check.mjs
```

The script writes results to `tools/performance/results/`.

Run browser timing checks from the frontend repo:

```bash
cd KEOnline
BASE_URL='http://127.0.0.1:5173' \
EMPLOYEE_ID='SUPER_ADMIN_001' \
PASSWORD='admin123' \
MOBILE=1 \
CAPTURE_SCREENSHOTS=1 \
  node qa-artifacts/performance-100k/browser-performance-check.mjs
```

For a deployed frontend, set `BASE_URL` to the deployed app URL. For desktop, omit `MOBILE=1`.

## Step 3: Capture Database Evidence

Enable or inspect:

- PostgreSQL slow query log.
- Backend request logs.
- JVM CPU/memory.
- DB CPU/memory.
- `EXPLAIN (ANALYZE, BUFFERS)` for slow SQL.

High-risk query shapes:

- Latest tickets by `created_at`.
- Mobile customer lookup by `mobile_number, created_at`.
- Customer history by `mobile_number`.
- `LIKE '%query%'` search across ticket number, mobile, customer, product, village.
- Filtered list by status/date/owner.

## Step 4: Test Candidate Indexes

After recording baseline results, apply candidate indexes:

```bash
DATABASE_URL='postgresql://user:password@host:5432/dbname' \
  psql "$DATABASE_URL" -f tools/performance/suggested-ticket-indexes.sql
```

Then rerun the API benchmark and compare p95/p99.

## Step 5: Browser Validation

Use a mobile viewport and desktop viewport for:

- Dashboard.
- Create Ticket.
- Create Ticket mobile lookup.
- Find Tickets default list.
- Search by mobile number.
- Search by customer name.
- Filter by status/date.
- Ticket detail.
- Customer history.

Check:

- No frozen input.
- No long blank loading state.
- No huge DOM list.
- No horizontal overflow.
- No large unpaginated network payload.

## Recommended Report Format

For each run, capture:

- Environment URL.
- Ticket count.
- Branch/commit.
- Database size.
- API p50/p95/p99.
- Slowest endpoints.
- Slowest SQL.
- Browser observations.
- Indexes applied.
- Recommended fixes.

## Expected First Bottlenecks

Likely bottlenecks at 100k:

- Wildcard ticket search.
- Missing `mobile_number, created_at` index for lookup/history.
- Unpaginated or overly large ticket responses.
- Total charge aggregation for ticket lists.
- Frontend rendering too many cards if pagination is bypassed.
