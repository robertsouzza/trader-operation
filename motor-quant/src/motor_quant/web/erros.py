"""Handlers de exceção padronizados em pt-BR, no mesmo formato do nucleo-backend."""

from __future__ import annotations

import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)


def _resposta(mensagem: str, codigo: int) -> JSONResponse:
    return JSONResponse(status_code=codigo, content={"mensagem": mensagem, "codigo": codigo})


def registrar_handlers(app: FastAPI) -> None:
    @app.exception_handler(HTTPException)
    async def _http(_request: Request, exc: HTTPException) -> JSONResponse:
        mensagem = exc.detail if isinstance(exc.detail, str) else "Erro HTTP."
        return _resposta(mensagem, exc.status_code)

    @app.exception_handler(RequestValidationError)
    async def _validacao(_request: Request, exc: RequestValidationError) -> JSONResponse:
        primeiro = exc.errors()[0] if exc.errors() else {}
        mensagem = primeiro.get("msg", "Requisição inválida.")
        campo = ".".join(str(parte) for parte in primeiro.get("loc", []))
        return _resposta(f"{campo}: {mensagem}" if campo else str(mensagem), 422)

    @app.exception_handler(Exception)
    async def _generico(_request: Request, exc: Exception) -> JSONResponse:
        logger.exception("Erro inesperado no motor-quant", exc_info=exc)
        return _resposta("Erro interno do servidor.", 500)
