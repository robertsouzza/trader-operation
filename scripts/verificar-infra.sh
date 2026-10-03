#!/usr/bin/env bash
# Definition of Done da skill 00: verifica Postgres (+pgvector), Redis e S3.
# Uso: ./scripts/verificar-infra.sh   (na raiz do projeto, com o compose no ar)
set -u

cd "$(dirname "$0")/.."
[ -f .env ] && set -a && . ./.env && set +a

PG_USER="${TRADER_POSTGRES_USER:-trader}"
PG_DB="${TRADER_POSTGRES_DB:-trader}"
BUCKET="${TRADER_S3_BUCKET:-trader-arquivos}"
falhas=0

ok()    { echo "OK    $1"; }
falha() { echo "FALHA $1"; falhas=$((falhas + 1)); }

versao_vector=$(docker compose exec -T postgres \
  psql -U "$PG_USER" -d "$PG_DB" -tAc "SELECT extversion FROM pg_extension WHERE extname = 'vector'" 2>/dev/null | tr -d '[:space:]')
if [ -n "$versao_vector" ]; then ok "Postgres no ar com pgvector $versao_vector"; else falha "Postgres ou extensão pgvector"; fi

if [ "$(docker compose exec -T redis redis-cli ping 2>/dev/null | tr -d '[:space:]')" = "PONG" ]; then
  ok "Redis respondeu PONG"
else
  falha "Redis não respondeu"
fi

if docker compose exec -T s3 wget -q -O /dev/null http://127.0.0.1:8333/healthz 2>/dev/null; then
  ok "S3 (SeaweedFS) saudável"
else
  falha "S3 (SeaweedFS) não está saudável"
fi

if docker compose run --rm --no-deps -T --entrypoint aws s3-init \
     --endpoint-url http://s3:8333 s3api head-bucket --bucket "$BUCKET" >/dev/null 2>&1; then
  ok "Bucket $BUCKET existe"
else
  falha "Bucket $BUCKET não encontrado (o s3-init rodou?)"
fi

if [ "$falhas" -eq 0 ]; then
  echo "Infraestrutura pronta."
else
  echo "$falhas verificação(ões) falharam."
  exit 1
fi
