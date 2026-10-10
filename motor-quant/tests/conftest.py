"""Fixtures compartilhadas: FastAPI com repositório zerado e token conhecido."""

from __future__ import annotations

from collections.abc import Iterator
from pathlib import Path

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from motor_quant.config import Settings
from motor_quant.infra.repositorio_memoria import RepositorioMemoria
from motor_quant.main import criar_app

TOKEN_TESTE = "token-de-teste-12345678"


@pytest.fixture
def repositorio() -> RepositorioMemoria:
    return RepositorioMemoria()


@pytest.fixture
def settings_teste() -> Settings:
    return Settings(
        shared_token=TOKEN_TESTE,
        seed="vazio",
        diretorio_seed=Path("data/seed-dev"),
    )


@pytest.fixture
def app(settings_teste: Settings, repositorio: RepositorioMemoria) -> FastAPI:
    return criar_app(settings=settings_teste, repositorio=repositorio)


@pytest.fixture
def cliente(app: FastAPI) -> Iterator[TestClient]:
    with TestClient(app) as cliente_http:
        yield cliente_http


@pytest.fixture
def cabecalhos_autorizados() -> dict[str, str]:
    return {"X-INTERNAL-TOKEN": TOKEN_TESTE}
