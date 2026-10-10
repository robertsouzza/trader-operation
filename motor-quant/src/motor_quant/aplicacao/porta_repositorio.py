"""Port de saída do motor-quant: contrato para quem armazena candles."""

from __future__ import annotations

from abc import ABC, abstractmethod
from collections.abc import Iterable, Sequence

from motor_quant.dominio.candle import Ativo, Candle, Timeframe


class CandleRepositorio(ABC):
    """Contrato do armazenamento de candles.

    Implementações: memória (skill 04) e, no futuro, Postgres quando o volume justificar.
    """

    @abstractmethod
    def listar(self, ativo: Ativo, timeframe: Timeframe, limite: int) -> Sequence[Candle]:
        """Devolve os `limite` candles mais recentes, em ordem crescente por `ts`."""

    @abstractmethod
    def anexar(
        self, ativo: Ativo, timeframe: Timeframe, candles: Iterable[Candle]
    ) -> int:
        """Adiciona `candles` ao par, deduplicando por `ts`. Devolve quantos novos entraram."""

    @abstractmethod
    def contar_pares(self) -> int:
        """Quantos pares (ativo, timeframe) têm pelo menos um candle."""
