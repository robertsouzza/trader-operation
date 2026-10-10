"""Testes das rotas de candles."""

from __future__ import annotations

from datetime import UTC, datetime

from fastapi.testclient import TestClient


def _ingerir(cliente: TestClient, cabecalhos: dict[str, str], quantidade: int) -> None:
    base = datetime(2026, 10, 10, 12, 0, tzinfo=UTC)
    candles = [
        {
            "ts": base.replace(minute=i).isoformat().replace("+00:00", "Z"),
            "abertura": 100.0,
            "maxima": 101.0,
            "minima": 99.0,
            "fechamento": 100.5,
            "volume": 1,
        }
        for i in range(quantidade)
    ]
    r = cliente.post(
        "/api/motor/ingest/candles",
        headers=cabecalhos,
        json={"ativo": "NAS100", "timeframe": "M1", "candles": candles},
    )
    assert r.status_code == 202, r.text
    assert r.json()["inseridos"] == quantidade


def test_listar_sem_token_devolve_401(cliente: TestClient) -> None:
    resposta = cliente.get("/api/motor/candles", params={"ativo": "NAS100", "timeframe": "M1"})
    assert resposta.status_code == 401


def test_listar_devolve_vazio_quando_repositorio_zerado(
    cliente: TestClient, cabecalhos_autorizados: dict[str, str]
) -> None:
    resposta = cliente.get(
        "/api/motor/candles",
        headers=cabecalhos_autorizados,
        params={"ativo": "NAS100", "timeframe": "M1"},
    )
    assert resposta.status_code == 200
    assert resposta.json() == []


def test_ingestao_adiciona_e_lista_devolve(
    cliente: TestClient, cabecalhos_autorizados: dict[str, str]
) -> None:
    _ingerir(cliente, cabecalhos_autorizados, quantidade=5)

    resposta = cliente.get(
        "/api/motor/candles",
        headers=cabecalhos_autorizados,
        params={"ativo": "NAS100", "timeframe": "M1", "limite": 3},
    )
    assert resposta.status_code == 200
    corpo = resposta.json()
    assert len(corpo) == 3


def test_ingestao_dedupe_na_segunda_chamada(
    cliente: TestClient, cabecalhos_autorizados: dict[str, str]
) -> None:
    _ingerir(cliente, cabecalhos_autorizados, quantidade=3)
    _ingerir_zero = cliente.post(
        "/api/motor/ingest/candles",
        headers=cabecalhos_autorizados,
        json={
            "ativo": "NAS100",
            "timeframe": "M1",
            "candles": [
                {
                    "ts": "2026-10-10T12:00:00Z",
                    "abertura": 100.0,
                    "maxima": 101.0,
                    "minima": 99.0,
                    "fechamento": 100.0,
                    "volume": 1,
                }
            ],
        },
    )
    assert _ingerir_zero.status_code == 202
    assert _ingerir_zero.json()["inseridos"] == 0


def test_ingestao_valida_ohlc_coerente(
    cliente: TestClient, cabecalhos_autorizados: dict[str, str]
) -> None:
    resposta = cliente.post(
        "/api/motor/ingest/candles",
        headers=cabecalhos_autorizados,
        json={
            "ativo": "NAS100",
            "timeframe": "M1",
            "candles": [
                {
                    "ts": "2026-10-10T12:00:00Z",
                    "abertura": 100.0,
                    "maxima": 90.0,  # abertura > maxima
                    "minima": 80.0,
                    "fechamento": 85.0,
                    "volume": 1,
                }
            ],
        },
    )
    assert resposta.status_code == 422
