"""Modelos de domínio de candles."""

from __future__ import annotations

from datetime import datetime
from typing import Literal, Self

from pydantic import BaseModel, ConfigDict, Field, model_validator

Ativo = Literal["NAS100", "XAUUSD"]
Timeframe = Literal["M1", "M5", "M15", "H1", "D1"]


class Candle(BaseModel):
    """Candle OHLCV num instante UTC específico."""

    model_config = ConfigDict(frozen=True, extra="forbid")

    ts: datetime = Field(..., description="Instante do fechamento do candle, em UTC.")
    abertura: float
    maxima: float
    minima: float
    fechamento: float
    volume: float = Field(..., ge=0)

    @model_validator(mode="after")
    def _validar_ohlc(self) -> Self:
        if self.minima > self.maxima:
            raise ValueError("minima não pode ser maior que maxima.")
        if not (self.minima <= self.abertura <= self.maxima):
            raise ValueError("abertura precisa estar entre minima e maxima.")
        if not (self.minima <= self.fechamento <= self.maxima):
            raise ValueError("fechamento precisa estar entre minima e maxima.")
        return self
