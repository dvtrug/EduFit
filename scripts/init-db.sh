#!/usr/bin/env sh
set -eu

docker compose up -d db

attempt=1
until docker compose exec -T db sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null 2>&1; do
  if [ "$attempt" -ge 30 ]; then
    echo "PostgreSQL did not become ready in time." >&2
    exit 1
  fi
  attempt=$((attempt + 1))
  sleep 2
done

docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" --set ON_ERROR_STOP=1 --file /docker-entrypoint-initdb.d/001-create-schema.sql'
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" --tuples-only --command "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ''edufit'';"'
