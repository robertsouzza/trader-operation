# motor-quant

Motor quantitativo do Trader Operation: Python 3.12 + FastAPI, armazenamento de candles em memória, indicadores (SMA, EMA, RSI, MACD) e ingestão por `POST` para o conector da skill 10. Entregue pela skill 04.

## Pré-requisitos

- Python 3.12 no WSL (`pyenv install 3.12.10` ou `sudo apt install python3.12 python3.12-venv python3.12-dev`).

## Rodar

```bash
cd motor-quant
python3.12 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"

export MOTOR_SHARED_TOKEN=dev-interno-trocar-em-producao
export TRADER_MOTOR_SEED=dev
uvicorn motor_quant.main:app --host 127.0.0.1 --port 8091 --reload
```

Scripts úteis dentro do venv:

```bash
pytest -q
ruff check src tests
mypy --strict src
```

## Fluxo

- `GET /healthz` — público; devolve status e quantos ativos foram carregados.
- `GET /api/motor/candles?ativo=NAS100&timeframe=M1&limite=200` — lista candles mais recentes.
- `POST /api/motor/ingest/candles` — ingestão em lote, dedupe por `ts`.
- `GET /api/motor/indicador?ativo=NAS100&timeframe=M1&nome=rsi&periodo=14` — SMA, EMA, RSI ou MACD.

Todas as rotas de negócio exigem o header `X-INTERNAL-TOKEN` (D-23); apenas `/healthz` é público. O token real nunca vai para o cliente final — é compartilhado só entre serviços internos.
