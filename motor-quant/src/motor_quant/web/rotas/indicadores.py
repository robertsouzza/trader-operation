"""Rota de indicadores: calcula SMA, EMA, RSI ou MACD sobre candles do repositório."""

from __future__ import annotations

from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, Query, Request
from pydantic import BaseModel, Field

from motor_quant.aplicacao.porta_repositorio import CandleRepositorio
from motor_quant.aplicacao.servico import IndicadorServico, NomeIndicador
from motor_quant.dominio.candle import Ativo, Timeframe
from motor_quant.web.seguranca import exigir_token_interno


def _repositorio(request: Request) -> CandleRepositorio:
    return request.app.state.repositorio  # type: ignore[no-any-return]


router = APIRouter(
    prefix="/api/motor",
    tags=["indicadores"],
    dependencies=[Depends(exigir_token_interno)],
)


class PontoIndicadorJson(BaseModel):
    ts: datetime
    valor: float | None
    sinal: float | None = None
    histograma: float | None = None


class RespostaIndicador(BaseModel):
    nome: NomeIndicador
    periodo: int = Field(..., ge=1)
    serie: list[PontoIndicadorJson]


@router.get(
    "/indicador",
    response_model=RespostaIndicador,
    summary="Calcula um indicador sobre os candles do par",
)
def calcular(
    ativo: Annotated[Ativo, Query()],
    timeframe: Annotated[Timeframe, Query()],
    nome: Annotated[NomeIndicador, Query(description="sma, ema, rsi ou macd")],
    periodo: Annotated[int, Query(ge=2, le=200)] = 14,
    limite: Annotated[int, Query(ge=2, le=2000)] = 500,
    repositorio: Annotated[CandleRepositorio, Depends(_repositorio)] = ...,  # type: ignore[assignment]
) -> RespostaIndicador:
    servico = IndicadorServico(repositorio)
    resultado = servico.calcular(ativo, timeframe, nome, periodo, limite)
    return RespostaIndicador(
        nome=resultado.nome,
        periodo=resultado.periodo,
        serie=[
            PontoIndicadorJson(
                ts=p.ts, valor=p.valor, sinal=p.sinal, histograma=p.histograma
            )
            for p in resultado.serie
        ],
    )
