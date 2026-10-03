---
name: trader-00-infra-base
description: Cria a base do monorepo Trader Operation — pastas das seis partes do sistema, .gitignore, .env.example, README, CLAUDE.md e docker-compose.yml só com infraestrutura (PostgreSQL 16 + pgvector, Redis, S3 via SeaweedFS). Primeira skill do roadmap; roda antes de qualquer outra.
---

# trader-00-infra-base — Monorepo + infraestrutura base

## Contexto

Primeira skill do roadmap. Cria o esqueleto do monorepo e sobe só os serviços de infraestrutura em Docker, sem código de aplicação, para validar o ambiente local (WSL2 + Docker) antes de gerar código.

**Assume:** nada.

## Referências obrigatórias

- `../trader-fullstack/SKILL.md`
- `../trader-fullstack/references/decisoes-tomadas.md` — D-06 (stack), D-08 (infra), D-13 (WSL), D-14 (versões)
- `../trader-fullstack/references/arquitetura.md` — partes do sistema

## Passos

1. **Pastas na raiz**, cada uma com um `README.md` de uma linha dizendo o que vai conter e qual skill a preenche:
   ```
   nucleo-backend/   (skill 01)
   motor-quant/      (skill 04)
   frontend-app/     (skill 03)
   copiloto/         (skill 11)
   conector/         (skill 10)
   mql5/             (skill 10)
   scripts/
   skills/
   ```
2. **`.gitignore`** cobrindo Java/Maven, Node, Python (`.venv/`, `__pycache__/`), Electron (`out/`, `release/`), MQL5 compilado (`*.ex5`), `.env`, logs, IDE e OS. Não ignorar `docker-compose.yml`.
3. **`.env.example`** com as portas e credenciais de dev usadas pelo compose, todas com valor padrão.
4. **`docker-compose.yml`** com:
   - `postgres`: `pgvector/pgvector:<tag pg16 explícita>`, usuário/banco `trader`, senha só de dev, volume `trader_pgdata`, healthcheck `pg_isready`, script em `infra/postgres/` que roda `CREATE EXTENSION IF NOT EXISTS vector;`.
   - `redis`: `redis:7-alpine` com tag explícita, healthcheck `redis-cli ping`.
   - `s3`: `chrislusf/seaweedfs` com tag explícita, API S3 na porta 8333, credenciais de dev em `infra/seaweedfs/s3.json` (D-08: MinIO saiu do Docker Hub).
   - `s3-init`: `amazon/aws-cli` com tag explícita; cria o bucket privado `trader-arquivos` se não existir e sai.
   - Todas as portas do host sobrescrevíveis por variáveis `TRADER_*_PORT`.
5. **`scripts/verificar-infra.sh`**: verifica os três serviços e sai com código diferente de zero se algum falhar.
6. **`README.md`** da raiz: o que é, stack, como subir a infra, tabela "Estado atual".
7. **`CLAUDE.md`** da raiz: contexto persistente para o Claude Code (regras, onde estão as referências, "Onde paramos").

## Definition of Done (verificável)

Na raiz do projeto:

```bash
cp .env.example .env
docker compose up -d postgres redis s3 s3-init
./scripts/verificar-infra.sh
```

O script deve imprimir `OK` para:
- Postgres respondendo e `SELECT extversion FROM pg_extension WHERE extname = 'vector'` devolvendo uma versão.
- Redis respondendo `PONG`.
- S3 (SeaweedFS) saudável em `/healthz` e bucket `trader-arquivos` existente.

`docker compose down` para tudo sem erro.

## Notas para as próximas skills

- Skill 01 adiciona o serviço `nucleo-backend` neste compose com `depends_on` em `postgres`, `redis` e `s3-init`.
- Skill 04 adiciona o serviço `motor-quant`.
- Usuários de banco além do owner são criados pelo Flyway do `nucleo-backend`, não aqui.
