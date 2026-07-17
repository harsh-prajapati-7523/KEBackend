# Manual database migrations

The project currently uses Hibernate `ddl-auto=update` and has no Flyway baseline/history. Phase 2A therefore uses a versioned, idempotent manual PostgreSQL migration.

Before deploying the matching application build, back up the database and run:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f db/migrations/V20260717_01__warranty_phase_2a.sql
```

Apply to test first, verify startup and warranty tests, then apply the same file to production. Record the filename, checksum, database, operator, and execution time in the deployment log. Do not use Hibernate schema generation as the production migration mechanism for these tables.

Phase 2B must be applied after Phase 2A:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f db/migrations/V20260717_02__warranty_phase_2b_workflow.sql
```

Phase 2B is authorized for controlled test deployment only. Production application is not authorized by this task.

Phase 2C test migration (after tester approval only):

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f db/migrations/V20260717_03__warranty_phase_2c_resolution.sql
```

Do not apply Phase 2C to production without a separate production-delivery authorization.

Phase 2D test migration (after Phase 2C and tester approval only):

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f db/migrations/V20260717_04__warranty_phase_2d_replacement.sql
```

Do not apply Phase 2D to production without separate authorization.

Phase 3 test migration (after Phase 2D and tester approval only):

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f db/migrations/V20260717_05__warranty_phase_3_documents.sql
```

Configure a private `WARRANTY_FILE_STORAGE_ROOT` before enabling uploads. Database backup alone is no longer complete; back up the database and warranty file root as one recovery set. Do not apply Phase 3 to production without separate authorization.

Phase 4 test migration (after Phase 3):

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f db/migrations/V20260717_06__warranty_phase_4_tracker.sql
```

Use `EXPLAIN (ANALYZE, BUFFERS)` after representative tracker data is loaded. Do not apply Phase 4 to production without separate authorization.

Phase 5 test migration (after Phase 4):

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f db/migrations/V20260717_07__warranty_phase_5_reports.sql
```

Run representative report `EXPLAIN (ANALYZE, BUFFERS)` checks after loading test data. Do not apply Phase 5 to production without separate authorization.

Phase 6 test migration (after Phase 5):

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f db/migrations/V20260717_08__warranty_phase_6_reminders.sql
```

Configure the reminder enablement, cron, timezone, snooze, escalation, and long-pending settings before test startup. Run generation twice and verify the unique deduplication constraint. Do not apply Phase 6 to production without separate authorization.
