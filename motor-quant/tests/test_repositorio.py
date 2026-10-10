"""Testes do RepositorioMemoria."""

from __future__ import annotations

from datetime import UTC, datetime

from motor_quant.dominio.candle import Candle
from motor_quant.infra.repositorio_memoria import RepositorioMemoria


def _candle(minuto: int, preco: float = 100.0) -> Candle:
    ts = datetime(2026, 10, 10, 12, minuto, tzinfo=UTC)
    return Candle(
        ts=ts,
        abertura=preco,
        maxima=preco + 0.5,
        minima=preco - 0.5,
        fechamento=preco,
        volume=1.0,
    )


def test_anexar_insere_e_lista_em_ordem_crescente() -> None:
    repo = RepositorioMemoria()
    repo.anexar("NAS100", "M1", [_candle(2), _candle(0), _candle(1)])
    serie = repo.listar("NAS100", "M1", limite=10)

    assert len(serie) == 3
    tsts = [c.ts for c in serie]
    assert tsts == sorted(tsts)


def test_anexar_deduplica_por_ts() -> None:
    repo = RepositorioMemoria()
    inseridos_1 = repo.anexar("NAS100", "M1", [_candle(0), _candle(1)])
    inseridos_2 = repo.anexar("NAS100", "M1", [_candle(1), _candle(2)])

    assert inseridos_1 == 2
    assert inseridos_2 == 1  # ts=1 já existia
    assert len(repo.listar("NAS100", "M1", limite=10)) == 3


def test_listar_respeita_limite_devolvendo_os_mais_recentes() -> None:
    repo = RepositorioMemoria()
    repo.anexar("NAS100", "M1", [_candle(i) for i in range(10)])

    serie = repo.listar("NAS100", "M1", limite=3)
    assert [c.ts for c in serie] == [
        datetime(2026, 10, 10, 12, 7, tzinfo=UTC),
        datetime(2026, 10, 10, 12, 8, tzinfo=UTC),
        datetime(2026, 10, 10, 12, 9, tzinfo=UTC),
    ]


def test_contar_pares_so_conta_pares_com_candles() -> None:
    repo = RepositorioMemoria()
    assert repo.contar_pares() == 0

    repo.anexar("NAS100", "M1", [_candle(0)])
    repo.anexar("XAUUSD", "M5", [_candle(0)])
    assert repo.contar_pares() == 2


def test_pares_distintos_nao_se_misturam() -> None:
    repo = RepositorioMemoria()
    repo.anexar("NAS100", "M1", [_candle(0, 20000)])
    repo.anexar("NAS100", "M5", [_candle(0, 20000)])
    repo.anexar("XAUUSD", "M1", [_candle(0, 2500)])

    assert len(repo.listar("NAS100", "M1", 10)) == 1
    assert len(repo.listar("NAS100", "M5", 10)) == 1
    assert len(repo.listar("XAUUSD", "M1", 10)) == 1
    assert len(repo.listar("XAUUSD", "M5", 10)) == 0
