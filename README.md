# Trader Operation

Plataforma web de análise e operação em tempo real, no estilo TradingView, integrada ao **MetaTrader 5** e com **IA embutida**. O operador master e a IA publicam operações (entrada, stop e alvos) em ativos como Nasdaq (NAS100) e ouro (XAUUSD); clientes assinantes acompanham ao vivo, conversam entre si e replicam as operações no MT5 da própria máquina pelo **Copiloto IA**. Uma vitrine reúne estratégias validadas por backtest.

> **Aviso:** operar no mercado financeiro envolve risco de perda. A plataforma publica sinais; a decisão de executar é sempre do cliente (ver D-02 em [decisoes-tomadas.md](skills/trader-fullstack/references/decisoes-tomadas.md)). Nunca commitar `.env`, chaves de API ou senhas de conta de trading.

## Stack

| Parte | Tecnologia |
|---|---|
| `nucleo-backend/` | Java 21, Spring Boot 3.5, Spring Security (JWT em cookie httpOnly), WebSocket/STOMP, Flyway, ArchUnit |
| `motor-quant/` | Python 3.12, FastAPI, pandas, vectorbt, SDKs de IA (Claude padrão, OpenAI, Ollama) |
| `frontend-app/` | React 19, TypeScript, Vite, React Router, TanStack Query, Vitest, TradingView Lightweight Charts |
| `copiloto/` | Electron + React, roda no Windows do cliente |
| `conector/` | Python (fork do [metatrader-mcp-server](https://github.com/ariadng/metatrader-mcp-server)), Python do Windows |
| `mql5/` | Indicador MQL5 para desenhar a operação no gráfico do MT5 |
| Infra | PostgreSQL 16 + pgvector, Redis, S3 (SeaweedFS em dev), Docker Compose |

Arquitetura e responsabilidades: [arquitetura.md](skills/trader-fullstack/references/arquitetura.md).

## Pré-requisitos

- Windows com **WSL2** (Ubuntu) e **Docker** (Docker Desktop com integração WSL, ou Docker Engine dentro do WSL)
- Git
- Para as próximas skills: Java 21, Node 20+, Python 3.12 (no WSL) e Python 3.12 do Windows + MetaTrader 5 (para o conector)

## Subir a infraestrutura

Na raiz do projeto (`~/trader-operation` no WSL):

```bash
cp .env.example .env
docker compose up -d postgres redis s3 s3-init
./scripts/verificar-infra.sh
```

Saída esperada:

```
OK    Postgres no ar com pgvector 0.8.1
OK    Redis respondeu PONG
OK    S3 (SeaweedFS) saudável
OK    Bucket trader-arquivos existe
Infraestrutura pronta.
```

| Serviço | Endereço | Credenciais (dev) |
|---|---|---|
| PostgreSQL | `localhost:5434`, banco `trader` | `trader` / `trader_dev` |
| Redis | `localhost:6380` | sem senha |
| S3 (SeaweedFS) | `http://localhost:8333`, bucket `trader-arquivos` | `trader_dev` / `trader_dev_secret` |

### Núcleo (API)

Pelo Docker, junto com a infra: `docker compose up -d --build`. Ou direto no WSL, com a infra no ar:

```bash
cd nucleo-backend
./mvnw spring-boot:run      # profile dev, porta 8090
./mvnw verify               # testes, incluindo ArchUnit e Testcontainers
```

| Serviço | Endereço |
|---|---|
| API | `http://localhost:8090` |
| Swagger | `http://localhost:8090/swagger-ui.html` |
| Usuários de dev (sintéticos) | `admin@`, `master@`, `cliente@trader.local`, senha `trader123` |

Portas ocupadas? Ajuste `TRADER_*_PORT` no `.env`. Para parar: `docker compose down` (os dados ficam nos volumes; `docker compose down -v` apaga tudo).

## Como o projeto é construído

O sistema é gerado em **skills numeradas** do Claude Code, cada uma com Definition of Done verificável por comando. Índice, regras e convenção de commit: [skills/trader-fullstack/SKILL.md](skills/trader-fullstack/SKILL.md).

## Estado atual

| # | Skill | Status |
|---|---|---|
| 00 | [trader-00-infra-base](skills/trader-00-infra-base/SKILL.md) | ✅ concluída |
| 01 | [trader-01-nucleo-core](skills/trader-01-nucleo-core/SKILL.md) | ✅ concluída |
| 02 | [trader-02-planos-perfis](skills/trader-02-planos-perfis/SKILL.md) | ✅ concluída |
| 03 | [trader-03-frontend-core](skills/trader-03-frontend-core/SKILL.md) | ✅ concluída |
| 04 | [trader-04-motor-quant-base](skills/trader-04-motor-quant-base/SKILL.md) | ✅ concluída |
| 05–12 | ver [índice](skills/trader-fullstack/SKILL.md) | ⏳ pendentes |

## Licença

A definir.
