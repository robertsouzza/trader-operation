"""Dependency do FastAPI que valida o shared secret interno (D-23)."""

from __future__ import annotations

import secrets
from typing import Annotated

from fastapi import Depends, Header, HTTPException, Request, status


def _token_configurado(request: Request) -> str:
    token: str = request.app.state.settings.shared_token
    return token


def exigir_token_interno(
    token_configurado: Annotated[str, Depends(_token_configurado)],
    x_internal_token: Annotated[str | None, Header(alias="X-INTERNAL-TOKEN")] = None,
) -> None:
    """Falha com 401/403 se o header não vier ou não bater com o segredo configurado.

    Comparação por `secrets.compare_digest` para evitar timing attack.
    """
    if x_internal_token is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Cabeçalho X-INTERNAL-TOKEN ausente.",
        )
    if not secrets.compare_digest(x_internal_token, token_configurado):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Token interno inválido.",
        )
