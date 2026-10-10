---
name: trader-04-motor-quant-base
description: Cria o motor-quant do Trader Operation — Python 3.12 + FastAPI, pandas e numpy, arquitetura hexagonal lite (dominio, aplicacao, infra, web), armazenamento de candles em memória com seed sintético, endpoints para catálogo de candles, ingestão e indicadores (SMA, EMA, RSI, MACD + sinal), proteção por header shared secret X-INTERNAL-TOKEN em todas as rotas de negócio, Dockerfile e serviço no docker-compose. Quinta skill do roadmap; roda depois da trader-03-frontend-core.
---

# trader-04-motor-quant-base — FastAPI: dados de mercado, candles e indicadores

## Contexto

Cria o `motor-quant/` com o esqueleto do serviço quantitativo em Python 3.12 + FastAPI. Nesta skill o serviço vive em memória: carrega CSVs sintéticos de dev no startup, aceita ingestão por POST (para quando o conector real chegar na skill 10 começar a empurrar candles reais) e expõe indicadores sobre esses candles. O `nucleo-backend` **não** chama o motor-quant ainda nesta skill — isso vem com a skill 05 (operações) ou 07 (IA analista). Nada de backtest, nenhum adapter de IA aqui: a skill 04 só entrega a base de dados e indicadores.

**Assume:** skill 03 concluída. `docker compose up -d --build` sobe o backend e o frontend sem erro.

## Referências obrigatórias

- `../trader-fullstack/SKILL.md`
- `../trader-fullstack/references/decisoes-tomadas.md` — D-06 (stack), D-14 (versões), D-22 (fonte de dados = MT5 do master num VPS), D-23 (shared secret interno), D-24 (indicadores iniciais)
- `../trader-fullstack/references/arquitetura.md` — `motor-quant`
- `../trader-fullstack/references/visao-e-requisitos.md` — RF-03 (ativos NAS100 e XAUUSD), RF-10 (IA pedirá backtests), RNF-04 (motor de risco inegociável — orientação arquitetural, não entra aqui)

## Passos

1. **Projeto Python 3.12 em `motor-quant/`** com `pyproject.toml` (PEP 621), setuptools como backend. Dependências pinadas (D-14):
   - Runtime: `fastapi==0.115.6`, `uvicorn[standard]==0.34.0`, `pydantic==2.10.4`, `pydantic-settings==2.7.1`, `pandas==2.2.3`, `numpy==2.2.1`.
   - Dev: `pytest==8.3.4`, `pytest-asyncio==0.25.2`, `httpx==0.28.1`, `ruff==0.8.6`, `mypy==1.14.1`.
   - Scripts no `pyproject.toml`: entry point `motor-quant = motor_quant.main:executar`.

2. **Estrutura de pastas** em `motor-quant/src/motor_quant/`:
   ```
   motor_quant/
   ├── __init__.py
   ├── main.py                  (cria FastAPI app + roda uvicorn no __main__)
   ├── config.py                (Settings via pydantic-settings)
   ├── dominio/
   │   ├── __init__.py
   │   ├── candle.py            (modelos: Candle, Timeframe, Ativo)
   │   └── indicadores.py       (funções puras: sma, ema, rsi, macd — recebem numpy, retornam numpy)
   ├── aplicacao/
   │   ├── __init__.py
   │   ├── porta_repositorio.py (CandleRepositorio: ABC)
   │   └── servico.py           (CandleServico, IndicadorServico)
   ├── infra/
   │   ├── __init__.py
   │   ├── repositorio_memoria.py (dict em memória, thread-safe por Lock)
   │   └── seed.py              (carrega CSVs de data/seed-dev)
   └── web/
       ├── __init__.py
       ├── seguranca.py         (depende de X-INTERNAL-TOKEN; dependency do FastAPI)
       ├── erros.py             (manipuladores de exceção padronizados em pt-BR)
       └── rotas/
           ├── __init__.py
           ├── saude.py         (/healthz — único endpoint público)
           ├── candles.py       (/api/motor/candles e /api/motor/ingest/candles)
           └── indicadores.py   (/api/motor/indicador)
   ```

3. **Modelos de domínio** (`dominio/candle.py`):
   - `Ativo = Literal["NAS100", "XAUUSD"]` (lista fechada por enquanto; novos ativos exigem decisão explícita).
   - `Timeframe = Literal["M1", "M5", "M15", "H1", "D1"]`.
   - `Candle(BaseModel)` com `ts: datetime (UTC)`, `abertura: float`, `maxima: float`, `minima: float`, `fechamento: float`, `volume: float`. Validação de OHLC: `minima <= abertura, fechamento <= maxima`.

4. **Indicadores (D-24)** em `dominio/indicadores.py`, funções puras sobre `numpy.ndarray` de fechamentos:
   - `sma(fechamentos, periodo) -> ndarray` — média móvel simples; posições com janela insuficiente são `nan`.
   - `ema(fechamentos, periodo) -> ndarray` — EMA com smoothing `2/(periodo+1)`; seed = SMA do primeiro bloco.
   - `rsi(fechamentos, periodo=14) -> ndarray` — RSI de Wilder, 0–100.
   - `macd(fechamentos, rapida=12, lenta=26, sinal=9) -> dict{linha, sinal, histograma}` — todas ndarrays.
   - Nenhum efeito colateral, nenhum I/O, nenhum logging. Testável em isolamento contra valores conhecidos.

5. **Repositório em memória** (`infra/repositorio_memoria.py`):
   - `Dict[tuple[Ativo, Timeframe], list[Candle]]` com `threading.Lock`.
   - `listar(ativo, timeframe, limite) -> list[Candle]` devolve os `limite` mais recentes (ordem crescente por `ts`).
   - `anexar(ativo, timeframe, candles)` adiciona com dedupe por `ts`, mantendo a ordem.
   - Persistência real (Postgres) fica para quando o volume crescer; por ora, memória é suficiente e aceita 10k candles por par sem suar.

6. **Seed sintético** (`infra/seed.py` + `data/seed-dev/`):
   - Dois CSVs: `NAS100_M1.csv` e `XAUUSD_M1.csv`, com ~500 candles cada, gerados por random walk (seed fixo para reprodutibilidade). Aviso no topo: "DADOS SINTÉTICOS — somente dev".
   - Carregados no startup do FastAPI quando `TRADER_MOTOR_SEED=dev`. Em outros profiles, não carrega nada (fica à espera do conector).

7. **Segurança — shared secret (D-23)** em `web/seguranca.py`:
   - Dependency `exigir_token_interno()` lê o header `X-INTERNAL-TOKEN` e compara com `settings.shared_token` por `secrets.compare_digest`.
   - 401 (`Unauthorized`) se faltar; 403 (`Forbidden`) se não bater.
   - Aplicada em todas as rotas de negócio via `router = APIRouter(dependencies=[Depends(exigir_token_interno)])`. **Só** `/healthz` fica fora.
   - `MOTOR_SHARED_TOKEN` lido do ambiente (sem default inseguro — se não vier, falha no startup). Em dev local, o `.env` tem um token qualquer; em produção, segredo rotacionado.

8. **Endpoints**:
   - `GET /healthz` → `{"status": "ok", "ativos_carregados": N}`. Público.
   - `GET /api/motor/candles?ativo=NAS100&timeframe=M1&limite=200` → lista de candles. `limite` entre 1 e 2000, default 200.
   - `POST /api/motor/ingest/candles` body `{ativo, timeframe, candles: [...]}` → 202, dedupe por `ts`. Preparado para a skill 10 (conector) empurrar lotes.
   - `GET /api/motor/indicador?ativo=NAS100&timeframe=M1&nome=sma&periodo=14` → `{nome, periodo, valor: float|null, serie: [{ts, valor}]}`. Nomes aceitos: `sma`, `ema`, `rsi`, `macd` (para `macd`, retorna também `sinal` e `histograma` na série).

9. **OpenAPI**: FastAPI gera `/docs` e `/openapi.json`. Descrições em português (BR); os modelos têm `description` nos campos.

10. **Erros**: `web/erros.py` registra handlers para `RequestValidationError` (422), `HTTPException` e `Exception` genérica — todos devolvem `{mensagem: str, codigo: int}` como o backend Spring faz.

11. **Testes com pytest** (`motor-quant/tests/`):
    - `test_indicadores.py` (unitário puro): SMA e EMA contra valores conhecidos; RSI em série sintética; MACD compatível com `pandas.ewm`.
    - `test_repositorio.py`: dedupe por `ts`, ordem crescente, limite.
    - `test_api_saude.py`: `/healthz` responde 200 e inclui `ativos_carregados`.
    - `test_api_seguranca.py`: `/api/motor/candles` sem header → 401; com header errado → 403; com header certo → 200.
    - `test_api_candles.py`: lista do seed, limite respeitado; ingestão por POST adiciona ao repositório e aparece em GET subsequente.
    - `test_api_indicador.py`: SMA(14) devolve série do mesmo tamanho; valores finais batem com cálculo manual.
    - Fixture compartilhada em `conftest.py` monta um FastAPI com repositório zerado + token conhecido via `TestClient` do `httpx`.

12. **Lint e type-check**:
    - `ruff check` com config no `pyproject.toml` (regras `E`, `F`, `I`, `UP`, `B`).
    - `mypy --strict` sobre `src/motor_quant/`.
    - Scripts opcionais: `make lint`, `make test`.

13. **Dockerfile + compose**:
    - `motor-quant/Dockerfile`: single-stage `python:3.12-slim`, `pip install .`, roda `uvicorn motor_quant.main:app --host 0.0.0.0 --port 8091`. Usuário não-root.
    - Serviço `motor-quant` no `docker-compose.yml`: porta `${TRADER_MOTOR_PORT:-8091}:8091`, env `MOTOR_SHARED_TOKEN` e `TRADER_MOTOR_SEED=dev`, healthcheck em `/healthz`. Sem `depends_on` em Postgres/Redis (não usa).
    - `.env.example` ganha `TRADER_MOTOR_PORT=8091` e `MOTOR_SHARED_TOKEN=dev-interno-trocar-em-producao`.

14. **Backend não chama o motor-quant** nesta skill. Nenhuma mudança em `nucleo-backend/` nem em `frontend-app/`.

## Definition of Done (verificável)

```bash
# 1. Testes locais
cd motor-quant
python3.12 -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
pytest -q
ruff check src tests
mypy --strict src
deactivate
cd ..

# 2. Build e sobe tudo
docker compose up -d --build

# 3. Saúde pública
curl -sf http://localhost:8091/healthz | jq '.ativos_carregados >= 2'

# 4. Sem token → 401
echo "sem token: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8091/api/motor/candles)"

# 5. Token errado → 403
echo "token errado: $(curl -s -o /dev/null -w '%{http_code}' -H 'X-INTERNAL-TOKEN: xyz' http://localhost:8091/api/motor/candles)"

# 6. Lista candles do seed
TOKEN=$(grep '^MOTOR_SHARED_TOKEN=' .env | cut -d= -f2)
curl -sf -H "X-INTERNAL-TOKEN: $TOKEN" \
  "http://localhost:8091/api/motor/candles?ativo=NAS100&timeframe=M1&limite=5" | jq 'length'  # 5

# 7. RSI(14) sobre NAS100
curl -sf -H "X-INTERNAL-TOKEN: $TOKEN" \
  "http://localhost:8091/api/motor/indicador?ativo=NAS100&timeframe=M1&nome=rsi&periodo=14" | jq '.serie | length'

# 8. Ingestão por POST
curl -sf -H "X-INTERNAL-TOKEN: $TOKEN" -H "Content-Type: application/json" \
  -X POST -d '{"ativo":"XAUUSD","timeframe":"M1","candles":[{"ts":"2026-10-10T12:00:00Z","abertura":2500,"maxima":2510,"minima":2495,"fechamento":2505,"volume":1}]}' \
  http://localhost:8091/api/motor/ingest/candles | jq '.inseridos'  # 1

# 9. Backend ainda OK
./scripts/verificar-infra.sh
cd nucleo-backend && ./mvnw -q test | tail -5 && cd ..
```

Todos devem passar. ArchUnit do backend continua verde, frontend sem regressão.

## Pré-requisitos na máquina de dev

Python 3.12 no WSL. Instalar se faltar: `sudo apt install python3.12 python3.12-venv python3.12-dev`.

## Notas para as próximas skills

- **Skill 05 (operações)** adiciona um adapter em `nucleo-backend` para falar com `/api/motor/candles` e `/api/motor/indicador` via o shared secret.
- **Skill 07 (IA analista)** registra ferramentas da IA que delegam para esses endpoints (`ler_cotacoes`, `calcular_indicador`).
- **Skill 08 (estratégias / backtest)** adiciona `vectorbt` ao `motor-quant`, novos endpoints `/api/motor/backtest`, e persiste em Postgres (quando o volume justificar).
- **Skill 10 (conector-mt5)** passa a POSTar candles reais em `/api/motor/ingest/candles` do VPS Windows, substituindo o seed sintético quando `TRADER_MOTOR_SEED=producao`.
- Autenticação por usuário final nunca chega ao motor-quant: o shared secret resolve a autenticação entre serviços, o `nucleo-backend` continua guardando quem é o usuário.
