# Manual database migrations

The project currently uses Hibernate `ddl-auto=update` and has no Flyway baseline/history. Phase 2A therefore uses a versioned, idempotent manual PostgreSQL migration.

Before deploying the matching application build, back up the database and run:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f db/migrations/V20260717_01__warranty_phase_2a.sql
```

Apply to test first, verify startup and warranty tests, then apply the same file to production. Record the filename, checksum, database, operator, and execution time in the deployment log. Do not use Hibernate schema generation as the production migration mechanism for these tables.
