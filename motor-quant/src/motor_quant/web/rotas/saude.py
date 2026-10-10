"""Endpoint público de saúde."""

from __future__ import annotations

from fastapi import APIRouter, Request
from pydantic import BaseModel

router = APIRouter(tags=["saude"])


class RespostaSaude(BaseModel):
    status: str
    ativos_carregados: int


@router.get("/healthz", response_model=RespostaSaude, summary="Saúde do motor-quant")
async def healthz(request: Request) -> RespostaSaude:
    try:
        repositorio = request.app.state.repositorio
        ativos = repositorio.contar_pares()
    except AttributeError:
        ativos = 0
    return RespostaSaude(status="ok", ativos_carregados=ativos)
