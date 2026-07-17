# Warranty file storage operations

Warranty uploads use a private filesystem root configured with `WARRANTY_FILE_STORAGE_ROOT`. Use separate roots such as `/var/lib/ke-ticket/test/warranty-files` and `/var/lib/ke-ticket/prod/warranty-files`; never place them under `/opt/keonline`, `/var/www`, a checkout, or a build directory.

Before enabling test uploads, create the root owned by the backend service user with mode `0750`, set `WARRANTY_FILE_STORAGE_ENABLED=true`, configure Spring's 8 MB multipart limit, and set Nginx `client_max_body_size` to at least 9 MB. Verify the test and production roots resolve to different real paths.

## Backup and restore

Database backup alone is not a complete warranty backup. Stop or quiesce document writes, create the PostgreSQL custom-format dump, and snapshot/archive the warranty root in the same maintenance window. Record both checksums and one shared recovery-set identifier. Store test and production backups separately with mode `0600`.

Restore the database and matching file archive as one set, restore ownership/mode, then verify a sample of active and inactive attachment checksums through authenticated access. Missing files must be treated as recovery incidents, not silently removed from metadata.

Inactive files are retained for `WARRANTY_FILE_RETENTION_DAYS` (default 90). Permanent cleanup is intentionally not scheduled in Phase 3; an operator must first reconcile inactive database rows against files, retain an audit log, and use a separately reviewed cleanup procedure. Orphan reconciliation and cloud migration remain deferred.
