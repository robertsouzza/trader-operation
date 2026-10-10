"""Gera CSVs sintéticos de desenvolvimento (random walk reprodutível).

Uso:
    python -m motor_quant.infra.gerar_seed

Gera data/seed-dev/NAS100_M1.csv e XAUUSD_M1.csv com ~500 candles cada.
"""

from __future__ import annotations

import csv
from datetime import UTC, datetime, timedelta
from pathlib import Path

import numpy as np

AVISO = (
    "# DADOS SINTÉTICOS — gerados por random walk reprodutível para desenvolvimento.\n"
    "# NÃO são preços reais de NAS100 nem XAUUSD. Não use para operar.\n"
)


def gerar(
    diretorio: Path,
    quantidade: int = 500,
    inicio: datetime | None = None,
) -> None:
    diretorio.mkdir(parents=True, exist_ok=True)
    inicio = inicio or datetime(2026, 10, 1, tzinfo=UTC)
    _gerar_arquivo(diretorio / "NAS100_M1.csv", quantidade, inicio, preco_base=20000, volatilidade=15.0, semente=1)
    _gerar_arquivo(diretorio / "XAUUSD_M1.csv", quantidade, inicio, preco_base=2500, volatilidade=1.5, semente=2)


def _gerar_arquivo(
    arquivo: Path,
    quantidade: int,
    inicio: datetime,
    preco_base: float,
    volatilidade: float,
    semente: int,
) -> None:
    rng = np.random.default_rng(semente)
    passos = rng.normal(loc=0.0, scale=volatilidade, size=quantidade)
    fechamentos = preco_base + np.cumsum(passos)

    with arquivo.open("w", encoding="utf-8", newline="") as fh:
        fh.write(AVISO)
        escritor = csv.writer(fh)
        escritor.writerow(["ts", "abertura", "maxima", "minima", "fechamento", "volume"])
        fechamento_anterior = preco_base
        for i, fechamento in enumerate(fechamentos):
            ts = inicio + timedelta(minutes=i)
            abertura = fechamento_anterior
            ruido = float(rng.normal(0, volatilidade / 3))
            maxima = max(abertura, float(fechamento)) + abs(ruido)
            minima = min(abertura, float(fechamento)) - abs(ruido)
            volume = float(rng.integers(10, 1000))
            escritor.writerow(
                [
                    ts.isoformat().replace("+00:00", "Z"),
                    f"{abertura:.5f}",
                    f"{maxima:.5f}",
                    f"{minima:.5f}",
                    f"{fechamento:.5f}",
                    f"{volume:.0f}",
                ]
            )
            fechamento_anterior = float(fechamento)


if __name__ == "__main__":
    gerar(Path(__file__).resolve().parents[3] / "data" / "seed-dev")
