"""Indicadores técnicos (D-24).

Funções puras sobre ``numpy.ndarray`` de fechamentos (dtype float). Nenhuma função
faz I/O ou logging; todas devolvem arrays do mesmo tamanho da entrada, com NaN
nas posições onde a janela ainda não enche.
"""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np
from numpy.typing import NDArray


def sma(fechamentos: NDArray[np.float64], periodo: int) -> NDArray[np.float64]:
    """Média móvel simples. Retorna NaN nas posições com janela insuficiente."""
    _validar_periodo(periodo)
    n = len(fechamentos)
    saida = np.full(n, np.nan, dtype=np.float64)
    if n < periodo:
        return saida
    # soma acumulada: SMA[i] = (soma[i+1] - soma[i+1-periodo]) / periodo
    acum = np.concatenate(([0.0], np.cumsum(fechamentos, dtype=np.float64)))
    for i in range(periodo - 1, n):
        saida[i] = (acum[i + 1] - acum[i + 1 - periodo]) / periodo
    return saida


def ema(fechamentos: NDArray[np.float64], periodo: int) -> NDArray[np.float64]:
    """EMA com smoothing 2/(periodo+1); seed = SMA do primeiro bloco de ``periodo``."""
    _validar_periodo(periodo)
    n = len(fechamentos)
    saida = np.full(n, np.nan, dtype=np.float64)
    if n < periodo:
        return saida
    alfa = 2.0 / (periodo + 1.0)
    saida[periodo - 1] = float(np.mean(fechamentos[:periodo]))
    for i in range(periodo, n):
        saida[i] = alfa * fechamentos[i] + (1 - alfa) * saida[i - 1]
    return saida


def rsi(fechamentos: NDArray[np.float64], periodo: int = 14) -> NDArray[np.float64]:
    """RSI de Wilder (0–100). NaN até o período encher."""
    _validar_periodo(periodo)
    n = len(fechamentos)
    saida = np.full(n, np.nan, dtype=np.float64)
    if n <= periodo:
        return saida
    deltas = np.diff(fechamentos)
    ganhos = np.where(deltas > 0, deltas, 0.0)
    perdas = np.where(deltas < 0, -deltas, 0.0)
    ganho_med = float(np.mean(ganhos[:periodo]))
    perda_med = float(np.mean(perdas[:periodo]))
    saida[periodo] = _rsi_do(ganho_med, perda_med)
    for i in range(periodo + 1, n):
        ganho_med = (ganho_med * (periodo - 1) + ganhos[i - 1]) / periodo
        perda_med = (perda_med * (periodo - 1) + perdas[i - 1]) / periodo
        saida[i] = _rsi_do(ganho_med, perda_med)
    return saida


@dataclass(frozen=True)
class ResultadoMacd:
    linha: NDArray[np.float64]
    sinal: NDArray[np.float64]
    histograma: NDArray[np.float64]


def macd(
    fechamentos: NDArray[np.float64],
    rapida: int = 12,
    lenta: int = 26,
    sinal: int = 9,
) -> ResultadoMacd:
    """MACD = EMA(rapida) − EMA(lenta); sinal = EMA(sinal) sobre a linha; histograma = linha − sinal."""
    _validar_periodo(rapida)
    _validar_periodo(lenta)
    _validar_periodo(sinal)
    if rapida >= lenta:
        raise ValueError("rapida precisa ser menor que lenta.")

    ema_rapida = ema(fechamentos, rapida)
    ema_lenta = ema(fechamentos, lenta)
    linha = (ema_rapida - ema_lenta).astype(np.float64)

    # EMA do sinal começa onde a linha começa a existir.
    inicio = int(np.argmax(~np.isnan(linha))) if np.any(~np.isnan(linha)) else len(linha)
    trecho_valido = linha[inicio:].astype(np.float64)
    sinal_valido = (
        ema(trecho_valido, sinal)
        if trecho_valido.size >= sinal
        else np.full(trecho_valido.size, np.nan, dtype=np.float64)
    )
    sinal_arr = np.full(len(linha), np.nan, dtype=np.float64)
    sinal_arr[inicio:] = sinal_valido
    histograma = (linha - sinal_arr).astype(np.float64)

    return ResultadoMacd(linha=linha, sinal=sinal_arr, histograma=histograma)


def _rsi_do(ganho_med: float, perda_med: float) -> float:
    if perda_med == 0:
        return 100.0
    rs = ganho_med / perda_med
    return 100.0 - (100.0 / (1.0 + rs))


def _validar_periodo(periodo: int) -> None:
    if periodo <= 0:
        raise ValueError(f"periodo precisa ser positivo, recebi {periodo}.")
