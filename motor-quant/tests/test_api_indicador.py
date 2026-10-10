"""Teste da rota de indicador contra candles ingeridos no fixture."""

from __future__ import annotations

from datetime import UTC, datetime

from fastapi.testclient import TestClient


def _ingerir_crescentes(
    cliente: TestClient, cabecalhos: dict[str, str], quantidade: int
) -> None:
    base = datetime(2026, 10, 10, 12, 0, tzinfo=UTC)
    candles = []
    for i in range(quantidade):
        preco = 100.0 + i
        candles.append(
            {
                "ts": base.replace(minute=i).isoformat().replace("+00:00", "Z"),
                "abertura": preco,
                "maxima": preco + 0.5,
                "minima": preco - 0.5,
                "fechamento": preco,
                "volume": 1,
            }
        )
    r = cliente.post(
        "/api/motor/ingest/candles",
        headers=cabecalhos,
        json={"ativo": "NAS100", "timeframe": "M1", "candles": candles},
    )
    assert r.status_code == 202, r.text


def test_indicador_sem_token_devolve_401(cliente: TestClient) -> None:
    resposta = cliente.get(
        "/api/motor/indicador",
        params={"ativo": "NAS100", "timeframe": "M1", "nome": "sma", "periodo": 3},
    )
    assert resposta.status_code == 401


def test_sma_sobre_serie_crescente(
    cliente: TestClient, cabecalhos_autorizados: dict[str, str]
) -> None:
    _ingerir_crescentes(cliente, cabecalhos_autorizados, quantidade=5)

    resposta = cliente.get(
        "/api/motor/indicador",
        headers=cabecalhos_autorizados,
        params={"ativo": "NAS100", "timeframe": "M1", "nome": "sma", "periodo": 3},
    )
    assert resposta.status_code == 200
    corpo = resposta.json()
    assert corpo["nome"] == "sma"
    assert corpo["periodo"] == 3
    serie = corpo["serie"]
    assert len(serie) == 5
    # Os dois primeiros não têm valor; os três últimos são 101, 102, 103.
    valores = [p["valor"] for p in serie]
    assert valores[:2] == [None, None]
    assert valores[2:] == [101.0, 102.0, 103.0]


def test_rsi_em_serie_crescente_satura_em_100(
    cliente: TestClient, cabecalhos_autorizados: dict[str, str]
) -> None:
    _ingerir_crescentes(cliente, cabecalhos_autorizados, quantidade=30)

    resposta = cliente.get(
        "/api/motor/indicador",
        headers=cabecalhos_autorizados,
        params={"ativo": "NAS100", "timeframe": "M1", "nome": "rsi", "periodo": 14},
    )
    assert resposta.status_code == 200
    serie = resposta.json()["serie"]
    valores = [p["valor"] for p in serie[14:]]
    assert all(v == 100.0 for v in valores if v is not None)


def test_macd_devolve_linha_sinal_e_histograma(
    cliente: TestClient, cabecalhos_autorizados: dict[str, str]
) -> None:
    _ingerir_crescentes(cliente, cabecalhos_autorizados, quantidade=60)

    resposta = cliente.get(
        "/api/motor/indicador",
        headers=cabecalhos_autorizados,
        params={"ativo": "NAS100", "timeframe": "M1", "nome": "macd", "periodo": 9},
    )
    assert resposta.status_code == 200
    serie = resposta.json()["serie"]
    assert len(serie) == 60
    for ponto in serie:
        assert "valor" in ponto
        assert "sinal" in ponto
        assert "histograma" in ponto
