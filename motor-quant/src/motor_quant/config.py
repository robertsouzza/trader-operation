"""Configuração do motor-quant lida do ambiente."""

from __future__ import annotations

from pathlib import Path
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict

PerfilSeed = Literal["dev", "producao", "vazio"]


class Settings(BaseSettings):
    """Configuração carregada do ambiente ou do arquivo .env (apenas em dev local)."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    shared_token: str
    """Segredo compartilhado entre nucleo-backend / conector / motor-quant. Obrigatório.

    Lido de MOTOR_SHARED_TOKEN. Sem default inseguro — se faltar, o processo não sobe."""

    seed: PerfilSeed = "vazio"
    """De onde o motor nasce com candles.

    - 'dev': carrega os CSVs sintéticos de data/seed-dev/.
    - 'producao': começa vazio e espera o conector (skill 10) empurrar por POST.
    - 'vazio': nada carregado (padrão de teste)."""

    diretorio_seed: Path = Path("data/seed-dev")
    """Pasta de CSVs quando seed == 'dev'."""

    @classmethod
    def do_ambiente(cls) -> Settings:
        """Construtor explícito — nunca lemos .env em teste; o teste injeta Settings direto."""
        return cls(
            shared_token=_obrigatorio("MOTOR_SHARED_TOKEN"),
            seed=_perfil_seed("TRADER_MOTOR_SEED"),
            diretorio_seed=Path(_opcional("TRADER_MOTOR_SEED_DIR", "data/seed-dev")),
        )


def _obrigatorio(nome: str) -> str:
    import os

    valor = os.environ.get(nome, "").strip()
    if not valor:
        raise RuntimeError(f"Variável de ambiente obrigatória não definida: {nome}")
    return valor


def _opcional(nome: str, padrao: str) -> str:
    import os

    return os.environ.get(nome, padrao).strip() or padrao


def _perfil_seed(nome: str) -> PerfilSeed:
    import os

    valor = os.environ.get(nome, "vazio").strip().lower()
    if valor not in ("dev", "producao", "vazio"):
        raise RuntimeError(
            f"{nome} inválido: {valor!r}. Valores aceitos: dev, producao, vazio."
        )
    return valor  # type: ignore[return-value]
