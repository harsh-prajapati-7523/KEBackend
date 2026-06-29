#!/usr/bin/env bash

set -euo pipefail

DB_HOST="${DB_HOST:-100.65.170.51}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-kedb_prod}"
DB_USERNAME="${DB_USERNAME:-ke_prod_user}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/ke-db-backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "ERROR: pg_dump is not installed or not available in PATH." >&2
  exit 1
fi

if [[ -z "${PGPASSWORD:-}" ]]; then
  if [[ -n "${DB_PASSWORD:-}" ]]; then
    export PGPASSWORD="$DB_PASSWORD"
  else
    echo "ERROR: Set DB_PASSWORD or PGPASSWORD before running this script." >&2
    exit 1
  fi
fi

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

timestamp="$(date +%Y%m%d_%H%M%S)"
backup_file="$BACKUP_DIR/${DB_NAME}_${timestamp}.dump"
latest_link="$BACKUP_DIR/${DB_NAME}_latest.dump"

echo "Starting backup"
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "Output: $backup_file"

pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USERNAME" \
  -d "$DB_NAME" \
  -F c \
  -f "$backup_file"

chmod 600 "$backup_file"
ln -sfn "$backup_file" "$latest_link"

echo "Backup complete"
ls -lh "$backup_file"

if [[ "$RETENTION_DAYS" =~ ^[0-9]+$ ]] && [[ "$RETENTION_DAYS" -gt 0 ]]; then
  find "$BACKUP_DIR" \
    -maxdepth 1 \
    -type f \
    -name "${DB_NAME}_*.dump" \
    -mtime +"$RETENTION_DAYS" \
    -print \
    -delete
fi

echo "Backup method: pg_dump custom format"
echo "Backup timestamp: $timestamp"
echo "Backup owner: $(whoami)"
echo "Backup success confirmation: created $backup_file"
