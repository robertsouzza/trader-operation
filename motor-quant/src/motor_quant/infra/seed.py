"""Carregamento de candles sintéticos para dev (D-22).

Lê os CSVs de ``diretorio/`` nomeados como ``<ATIVO>_<TIMEFRAME>.csv`` e enfileira
no repositório. Formato esperado (sem header obrigatório, mas aceito):

    ts,abertura,maxima,minima,fechamento,volume
    2026-10-01T12:00:00Z,100.0,101.0,99.5,100.5,1
    ...
"""

from __future__ import annotations

import csv
import logging
from collections.abc import Iterator
from datetime import datetime
from pathlib import Path
from typing import get_args

from motor_quant.aplicacao.porta_repositorio import CandleRepositorio
from motor_quant.dominio.candle import Ativo, Candle, Timeframe

logger = logging.getLogger(__name__)

ATIVOS_VALIDOS: frozenset[str] = frozenset(get_args(Ativo))
TIMEFRAMES_VALIDOS: frozenset[str] = frozenset(get_args(Timeframe))


def carregar_seed_dev(diretorio: Path, repositorio: CandleRepositorio) -> int:
    """Carrega todos os CSVs do diretório. Devolve o total de candles inseridos."""
    if not diretorio.exists():
        logger.warning("Diretório de seed não existe: %s", diretorio)
        return 0

    total = 0
    for arquivo in sorted(diretorio.glob("*.csv")):
        try:
            ativo, timeframe = _par_do_arquivo(arquivo)
        except ValueError as erro:
            logger.warning("Ignorando %s: %s", arquivo.name, erro)
            continue

        candles = list(_ler_candles(arquivo))
        if not candles:
            continue
        inseridos = repositorio.anexar(ativo, timeframe, candles)
        total += inseridos
        logger.info(
            "Seed: %d candles inseridos em (%s, %s) de %s",
            inseridos,
            ativo,
            timeframe,
            arquivo.name,
        )
    return total


def _par_do_arquivo(arquivo: Path) -> tuple[Ativo, Timeframe]:
    partes = arquivo.stem.split("_")
    if len(partes) != 2:
        raise ValueError("nome precisa ser <ATIVO>_<TIMEFRAME>.csv")
    ativo_raw, timeframe_raw = partes
    if ativo_raw not in ATIVOS_VALIDOS:
        raise ValueError(f"ativo desconhecido: {ativo_raw}")
    if timeframe_raw not in TIMEFRAMES_VALIDOS:
        raise ValueError(f"timeframe desconhecido: {timeframe_raw}")
    return ativo_raw, timeframe_raw  # type: ignore[return-value]


def _ler_candles(arquivo: Path) -> Iterator[Candle]:
    with arquivo.open(encoding="utf-8") as fh:
        linhas_uteis = (linha for linha in fh if not linha.startswith("#"))
        leitor = csv.DictReader(linhas_uteis)
        for linha in leitor:
            yield Candle(
                ts=datetime.fromisoformat(linha["ts"].replace("Z", "+00:00")),
                abertura=float(linha["abertura"]),
                maxima=float(linha["maxima"]),
                minima=float(linha["minima"]),
                fechamento=float(linha["fechamento"]),
                volume=float(linha["volume"]),
            )
