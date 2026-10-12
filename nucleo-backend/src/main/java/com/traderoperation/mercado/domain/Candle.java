package com.traderoperation.mercado.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Candle OHLCV no padrão do motor-quant (não persiste; é passado adiante como DTO). */
public record Candle(
        Instant ts,
        BigDecimal abertura,
        BigDecimal maxima,
        BigDecimal minima,
        BigDecimal fechamento,
        BigDecimal volume) {

    public Candle {
        Objects.requireNonNull(ts, "ts");
        Objects.requireNonNull(abertura, "abertura");
        Objects.requireNonNull(maxima, "maxima");
        Objects.requireNonNull(minima, "minima");
        Objects.requireNonNull(fechamento, "fechamento");
        Objects.requireNonNull(volume, "volume");
    }
}
