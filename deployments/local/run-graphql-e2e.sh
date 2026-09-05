#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)" && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:8080}"
GRAPHQL_URL="$BASE_URL/graphql"
E2E_EMAIL="${E2E_EMAIL:-gql-e2e-$(date -u +%Y%m%d%H%M%S)-$$@example.test}"
E2E_PASSWORD="${E2E_PASSWORD:-E2E-password-123}"

psql_gql() {
  "${COMPOSE[@]}" exec -T postgres psql -X -v ON_ERROR_STOP=1 -U DRAGON -d POS "$@"
}

sql_literal() {
  printf '%s' "$1" | sed "s/'/''/g"
}

cleanup() {
  local email_lit
  email_lit="$(sql_literal "$E2E_EMAIL")"
  psql_gql -c "DELETE FROM users WHERE email = '$email_lit';" >/dev/null 2>&1 || true
}
trap cleanup EXIT

wait_for_gateway() {
  for _ in $(seq 1 "${E2E_WAIT_ATTEMPTS:-60}"); do
    if curl --silent --fail --max-time 3 "$BASE_URL/health/ready" >/dev/null; then
      return 0
    fi
    sleep 2
  done
  echo "Gateway did not become ready: $BASE_URL/health/ready" >&2
  "${COMPOSE[@]}" ps >&2 || true
  exit 1
}

wait_for_gateway

# Register through the GraphQL endpoint
REGISTER_PAYLOAD=$(cat <<JSON
{"query":"mutation Reg(\\$f:String!,\\$l:String!,\\$e:String!,\\$p:String!){ register(firstname:\\$f,lastname:\\$l,email:\\$e,password:\\$p){ status message } }","variables":{"f":"GraphQL","l":"E2E","e":"$E2E_EMAIL","p":"$E2E_PASSWORD"}}
JSON
)
REGISTER_RESPONSE=$(curl --silent --show-error --max-time 10 \
  -H 'Content-Type: application/json' \
  -d "$REGISTER_PAYLOAD" \
  "$GRAPHQL_URL")
echo "$REGISTER_RESPONSE" | grep -q '"status":"success"' || {
  echo "GraphQL registration failed" >&2
  echo "$REGISTER_RESPONSE" >&2
  exit 1
}

# Verify account (activate without OTP — same shortcut as POS e2e)
psql_gql -At -c \
  "UPDATE users SET is_active = true WHERE email = '$(sql_literal "$E2E_EMAIL")';" \
  >/dev/null 2>&1 || true

E2E_EMAIL="$E2E_EMAIL" E2E_PASSWORD="$E2E_PASSWORD" \
  hurl --test \
  --variable base_url="$BASE_URL" \
  --variable graphql_url="$GRAPHQL_URL" \
  --variable e2e_email="$E2E_EMAIL" \
  --variable e2e_password="$E2E_PASSWORD" \
  "$ROOT_DIR/e2e-graphql.hurl"

echo "GraphQL E2E passed for $E2E_EMAIL"
