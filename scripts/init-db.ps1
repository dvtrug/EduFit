$ErrorActionPreference = "Stop"

docker compose up -d db
if ($LASTEXITCODE -ne 0) {
    throw "Could not start PostgreSQL."
}

for ($attempt = 1; $attempt -le 30; $attempt++) {
    docker compose exec -T db sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' | Out-Null
    if ($LASTEXITCODE -eq 0) {
        break
    }
    if ($attempt -eq 30) {
        throw "PostgreSQL did not become ready in time."
    }
    Start-Sleep -Seconds 2
}

docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" --set ON_ERROR_STOP=1 --file /docker-entrypoint-initdb.d/001-create-schema.sql'
if ($LASTEXITCODE -ne 0) {
    throw "Schema initialization failed."
}

docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" --tuples-only --command "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ''edufit'';"'
if ($LASTEXITCODE -ne 0) {
    throw "Schema verification failed."
}
