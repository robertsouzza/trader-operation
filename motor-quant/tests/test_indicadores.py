"""Testes unitários dos indicadores (funções puras sobre numpy)."""

from __future__ import annotations

import numpy as np
import pandas as pd
import pytest

from motor_quant.dominio.indicadores import ema, macd, rsi, sma


def _crescente(n: int) -> np.ndarray:
    return np.arange(1.0, n + 1, dtype=np.float64)


def test_sma_valores_conhecidos() -> None:
    valores = _crescente(5)  # [1, 2, 3, 4, 5]
    resultado = sma(valores, periodo=3)
    assert np.isnan(resultado[:2]).all()
    np.testing.assert_allclose(resultado[2:], [2.0, 3.0, 4.0])


def test_sma_periodo_maior_que_serie() -> None:
    resultado = sma(_crescente(3), periodo=5)
    assert np.isnan(resultado).all()


def test_ema_bate_com_pandas() -> None:
    rng = np.random.default_rng(42)
    valores = np.cumsum(rng.normal(0, 1, 200)) + 100
    periodo = 10
    resultado = ema(valores, periodo)
    esperado = (
        pd.Series(valores)
        .ewm(span=periodo, adjust=False)
        .mean()
        .to_numpy()
    )
    # Ignora os primeiros períodos-1 pontos (nossa EMA começa com SMA do primeiro bloco).
    np.testing.assert_allclose(resultado[periodo - 1 :], esperado[periodo - 1 :], atol=1.5)


def test_rsi_serie_sempre_crescente_fica_proximo_de_100() -> None:
    resultado = rsi(_crescente(50), periodo=14)
    # Entradas estritamente crescentes: perdas = 0 → RSI = 100.
    np.testing.assert_allclose(resultado[14:], 100.0)


def test_rsi_entrada_insuficiente() -> None:
    resultado = rsi(_crescente(10), periodo=14)
    assert np.isnan(resultado).all()


def test_macd_estrutura_e_relacao_entre_linhas() -> None:
    rng = np.random.default_rng(7)
    valores = np.cumsum(rng.normal(0, 1, 100)) + 50
    r = macd(valores, rapida=12, lenta=26, sinal=9)
    assert len(r.linha) == len(valores)
    assert len(r.sinal) == len(valores)
    assert len(r.histograma) == len(valores)
    # histograma = linha - sinal (ignorando NaNs)
    mascara = ~np.isnan(r.linha) & ~np.isnan(r.sinal)
    np.testing.assert_allclose(r.histograma[mascara], r.linha[mascara] - r.sinal[mascara])


@pytest.mark.parametrize("periodo", [0, -1])
def test_periodo_invalido_falha(periodo: int) -> None:
    with pytest.raises(ValueError):
        sma(_crescente(10), periodo)
