#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATABASE_URL="${DATABASE_URL:-${DB_URL:-}}"
TICKET_COUNT="${TICKET_COUNT:-100000}"
DELETE_EXISTING="${DELETE_EXISTING:-0}"
TICKET_PREFIX="${TICKET_PREFIX:-KE-PERF-}"

if [[ -z "$DATABASE_URL" ]]; then
  echo "Set DATABASE_URL or DB_URL before running this script." >&2
  exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required but was not found in PATH." >&2
  exit 1
fi

echo "Seeding ${TICKET_COUNT} performance tickets with prefix ${TICKET_PREFIX}"
echo "DELETE_EXISTING=${DELETE_EXISTING}"

psql "$DATABASE_URL" \
  -v ticket_count="$TICKET_COUNT" \
  -v delete_existing="$DELETE_EXISTING" \
  -v ticket_prefix="$TICKET_PREFIX" \
  -f "$SCRIPT_DIR/seed-100k-tickets.sql"

echo "Done."

