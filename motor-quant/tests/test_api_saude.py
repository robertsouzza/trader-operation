"""Teste do endpoint público de saúde."""

from __future__ import annotations

from fastapi.testclient import TestClient


def test_healthz_devolve_ok_e_contagem_de_pares(cliente: TestClient) -> None:
    resposta = cliente.get("/healthz")

    assert resposta.status_code == 200
    corpo = resposta.json()
    assert corpo["status"] == "ok"
    assert corpo["ativos_carregados"] == 0


def test_healthz_nao_exige_token(cliente: TestClient) -> None:
    # Qualquer requisição a /healthz deve passar sem header de autenticação.
    resposta = cliente.get("/healthz")
    assert resposta.status_code == 200
