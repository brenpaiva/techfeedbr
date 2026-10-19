#!/bin/sh
set -eu

# Run on the API host as an operator with Docker access. No endpoint is public.
output_dir=${1:-/srv/techfeed-br/evaluation-exports}
mkdir -p "$output_dir"
chmod 700 "$output_dir"
stamp=$(date -u +%Y%m%dT%H%M%SZ)
csv="$output_dir/techfeed-web-evaluations-$stamp.csv"
json="$output_dir/techfeed-web-evaluations-$stamp.json"
umask 077

docker exec techfeed-br-db-1 sh -c 'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -c "COPY (SELECT s.id AS session_id, s.session_code, s.task_code, e.event_type, e.occurred_at, e.elapsed_ms, e.metadata FROM web_evaluation_session s JOIN web_evaluation_event e ON e.session_id = s.id WHERE s.expires_at > now() ORDER BY s.started_at, e.occurred_at) TO STDOUT WITH CSV HEADER"' > "$csv"

docker exec techfeed-br-db-1 sh -c 'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -Atc "SELECT COALESCE(json_agg(row_to_json(x)), '\''[]'\''::json)::text FROM (SELECT s.id AS session_id, s.session_code, s.task_code, e.event_type, e.occurred_at, e.elapsed_ms, e.metadata FROM web_evaluation_session s JOIN web_evaluation_event e ON e.session_id = s.id WHERE s.expires_at > now() ORDER BY s.started_at, e.occurred_at) x"' > "$json"

printf 'CSV: %s\nJSON: %s\n' "$csv" "$json"
