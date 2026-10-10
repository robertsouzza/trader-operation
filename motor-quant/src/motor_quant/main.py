"""Boot do motor-quant: cria o FastAPI, registra rotas e dependências."""

from __future__ import annotations

import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from motor_quant.aplicacao.porta_repositorio import CandleRepositorio
from motor_quant.config import Settings
from motor_quant.infra.repositorio_memoria import RepositorioMemoria
from motor_quant.infra.seed import carregar_seed_dev
from motor_quant.web.erros import registrar_handlers
from motor_quant.web.rotas.candles import router as rotas_candles
from motor_quant.web.rotas.indicadores import router as rotas_indicadores
from motor_quant.web.rotas.saude import router as rotas_saude

logger = logging.getLogger(__name__)


def criar_app(
    settings: Settings | None = None,
    repositorio: CandleRepositorio | None = None,
) -> FastAPI:
    """Fábrica do FastAPI.

    Em teste, injete ``settings`` e ``repositorio`` para evitar o .env e começar vazio.
    Em produção, o uvicorn chama ``criar_app`` sem argumentos (``--factory``).
    """
    config = settings or Settings.do_ambiente()
    repo = repositorio or RepositorioMemoria()

    @asynccontextmanager
    async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
        if config.seed == "dev":
            inseridos = carregar_seed_dev(config.diretorio_seed, repo)
            logger.info("Seed de dev carregado: %d candles no total.", inseridos)
        yield

    app = FastAPI(
        title="motor-quant",
        version="0.1.0",
        description="Motor quantitativo do Trader Operation: candles e indicadores.",
        lifespan=lifespan,
    )
    app.state.settings = config
    app.state.repositorio = repo

    registrar_handlers(app)
    app.include_router(rotas_saude)
    app.include_router(rotas_candles)
    app.include_router(rotas_indicadores)
    return app


def executar() -> None:
    """Entry point do pacote: roda uvicorn em modo produção simples."""
    import uvicorn

    uvicorn.run(
        "motor_quant.main:criar_app",
        host="0.0.0.0",  # noqa: S104 -- este é o processo do serviço
        port=8091,
        factory=True,
    )
