"""Serviços de aplicação: calculam indicadores sobre candles do repositório."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Literal

import numpy as np

from motor_quant.aplicacao.porta_repositorio import CandleRepositorio
from motor_quant.dominio.candle import Ativo, Timeframe
from motor_quant.dominio.indicadores import ema, macd, rsi, sma

NomeIndicador = Literal["sma", "ema", "rsi", "macd"]


@dataclass(frozen=True)
class PontoIndicador:
    ts: datetime
    valor: float | None
    sinal: float | None = None
    histograma: float | None = None


@dataclass(frozen=True)
class ResultadoIndicador:
    nome: NomeIndicador
    periodo: int
    serie: list[PontoIndicador]


class IndicadorServico:
    """Calcula indicadores a partir de candles existentes no repositório."""

    def __init__(self, repositorio: CandleRepositorio) -> None:
        self._repositorio = repositorio

    def calcular(
        self,
        ativo: Ativo,
        timeframe: Timeframe,
        nome: NomeIndicador,
        periodo: int,
        limite: int = 500,
    ) -> ResultadoIndicador:
        candles = self._repositorio.listar(ativo, timeframe, limite)
        timestamps = [c.ts for c in candles]
        fechamentos = np.array([c.fechamento for c in candles], dtype=np.float64)

        if nome == "sma":
            valores = sma(fechamentos, periodo)
            serie = [
                PontoIndicador(ts=ts, valor=_opt(v))
                for ts, v in zip(timestamps, valores, strict=True)
            ]
        elif nome == "ema":
            valores = ema(fechamentos, periodo)
            serie = [
                PontoIndicador(ts=ts, valor=_opt(v))
                for ts, v in zip(timestamps, valores, strict=True)
            ]
        elif nome == "rsi":
            valores = rsi(fechamentos, periodo)
            serie = [
                PontoIndicador(ts=ts, valor=_opt(v))
                for ts, v in zip(timestamps, valores, strict=True)
            ]
        elif nome == "macd":
            # periodo aqui é o período do sinal; rápida/lenta ficam no padrão (12/26).
            resultado = macd(fechamentos, rapida=12, lenta=26, sinal=periodo)
            serie = [
                PontoIndicador(
                    ts=ts,
                    valor=_opt(linha),
                    sinal=_opt(sinal_v),
                    histograma=_opt(hist),
                )
                for ts, linha, sinal_v, hist in zip(
                    timestamps,
                    resultado.linha,
                    resultado.sinal,
                    resultado.histograma,
                    strict=True,
                )
            ]
        else:  # pragma: no cover — tipagem garante os literais acima
            raise ValueError(f"Indicador desconhecido: {nome}")

        return ResultadoIndicador(nome=nome, periodo=periodo, serie=serie)


def _opt(valor: float) -> float | None:
    """numpy devolve NaN; a API responde com null nos pontos sem dado suficiente."""
    if np.isnan(valor):
        return None
    return float(valor)
