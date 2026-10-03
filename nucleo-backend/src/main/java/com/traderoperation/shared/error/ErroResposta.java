package com.traderoperation.shared.error;

import java.time.Instant;
import java.util.Map;

/** Formato único de erro devolvido pela API. */
public record ErroResposta(Instant timestamp, int status, String erro, String mensagem, Map<String, String> campos) {

    public static ErroResposta de(int status, String erro, String mensagem) {
        return new ErroResposta(Instant.now(), status, erro, mensagem, Map.of());
    }
}
