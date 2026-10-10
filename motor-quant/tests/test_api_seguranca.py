"""Teste da dependency de shared secret contra uma rota protegida sintética.

Isola a verificação do token do resto da API: cria um FastAPI mínimo, pendura
``exigir_token_interno`` numa rota qualquer e cobre os três cenários (sem header,
header errado, header certo).
"""

from __future__ import annotations

from fastapi import Depends, FastAPI
from fastapi.testclient import TestClient

from motor_quant.config import Settings
from motor_quant.web.seguranca import exigir_token_interno


def _app_com_rota_protegida(token: str) -> FastAPI:
    app = FastAPI()
    app.state.settings = Settings(shared_token=token, seed="vazio")

    @app.get("/_protegido", dependencies=[Depends(exigir_token_interno)])
    def _rota() -> dict[str, bool]:
        return {"ok": True}

    return app


def test_sem_header_devolve_401() -> None:
    with TestClient(_app_com_rota_protegida("segredo-ok")) as cliente:
        resposta = cliente.get("/_protegido")
    assert resposta.status_code == 401


def test_header_errado_devolve_403() -> None:
    with TestClient(_app_com_rota_protegida("segredo-ok")) as cliente:
        resposta = cliente.get("/_protegido", headers={"X-INTERNAL-TOKEN": "errado"})
    assert resposta.status_code == 403


def test_header_certo_passa() -> None:
    with TestClient(_app_com_rota_protegida("segredo-ok")) as cliente:
        resposta = cliente.get("/_protegido", headers={"X-INTERNAL-TOKEN": "segredo-ok"})
    assert resposta.status_code == 200
    assert resposta.json() == {"ok": True}
