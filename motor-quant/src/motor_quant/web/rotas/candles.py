"""Rotas de candles — leitura e ingestão em lote."""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, Query, Request, status
from pydantic import BaseModel, Field

from motor_quant.aplicacao.porta_repositorio import CandleRepositorio
from motor_quant.dominio.candle import Ativo, Candle, Timeframe
from motor_quant.web.seguranca import exigir_token_interno


def _repositorio(request: Request) -> CandleRepositorio:
    return request.app.state.repositorio  # type: ignore[no-any-return]


router = APIRouter(
    prefix="/api/motor",
    tags=["candles"],
    dependencies=[Depends(exigir_token_interno)],
)


class LoteCandles(BaseModel):
    ativo: Ativo
    timeframe: Timeframe
    candles: list[Candle] = Field(..., min_length=1, max_length=10_000)


class RespostaIngestao(BaseModel):
    inseridos: int


@router.get(
    "/candles",
    response_model=list[Candle],
    summary="Lista os candles mais recentes de um par ativo/timeframe",
)
def listar(
    ativo: Annotated[Ativo, Query(description="NAS100 ou XAUUSD")],
    timeframe: Annotated[Timeframe, Query(description="M1, M5, M15, H1 ou D1")],
    limite: Annotated[int, Query(ge=1, le=2000)] = 200,
    repositorio: Annotated[CandleRepositorio, Depends(_repositorio)] = ...,  # type: ignore[assignment]
) -> list[Candle]:
    return list(repositorio.listar(ativo, timeframe, limite))


@router.post(
    "/ingest/candles",
    status_code=status.HTTP_202_ACCEPTED,
    response_model=RespostaIngestao,
    summary="Ingere um lote de candles (usado pelo conector MT5 da skill 10)",
)
def ingerir(
    lote: LoteCandles,
    repositorio: Annotated[CandleRepositorio, Depends(_repositorio)] = ...,  # type: ignore[assignment]
) -> RespostaIngestao:
    inseridos = repositorio.anexar(lote.ativo, lote.timeframe, lote.candles)
    return RespostaIngestao(inseridos=inseridos)
