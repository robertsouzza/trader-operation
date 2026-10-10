"""Implementação em memória do CandleRepositorio.

Thread-safe por `threading.Lock`. Mantém os candles ordenados por `ts` em cada par
(ativo, timeframe). Volume prático suportado: 10k candles por par sem problemas.
"""

from __future__ import annotations

import threading
from collections.abc import Iterable, Sequence

from motor_quant.aplicacao.porta_repositorio import CandleRepositorio
from motor_quant.dominio.candle import Ativo, Candle, Timeframe


class RepositorioMemoria(CandleRepositorio):
    def __init__(self) -> None:
        self._por_par: dict[tuple[Ativo, Timeframe], list[Candle]] = {}
        self._lock = threading.Lock()

    def listar(self, ativo: Ativo, timeframe: Timeframe, limite: int) -> Sequence[Candle]:
        if limite <= 0:
            return []
        with self._lock:
            serie = self._por_par.get((ativo, timeframe), [])
            return list(serie[-limite:])

    def anexar(
        self, ativo: Ativo, timeframe: Timeframe, candles: Iterable[Candle]
    ) -> int:
        novos = list(candles)
        if not novos:
            return 0
        with self._lock:
            serie = self._por_par.setdefault((ativo, timeframe), [])
            existentes = {c.ts for c in serie}
            inseridos = 0
            for candle in novos:
                if candle.ts in existentes:
                    continue
                serie.append(candle)
                existentes.add(candle.ts)
                inseridos += 1
            serie.sort(key=lambda c: c.ts)
            return inseridos

    def contar_pares(self) -> int:
        with self._lock:
            return sum(1 for serie in self._por_par.values() if serie)
