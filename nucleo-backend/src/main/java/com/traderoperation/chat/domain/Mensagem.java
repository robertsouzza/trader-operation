package com.traderoperation.chat.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Mensagem no chat de uma operação (RF-07). */
public record Mensagem(
        UUID id,
        UUID operacaoId,
        UUID autorId,
        String texto,
        Instant enviadaEm) {

    public static final int TAMANHO_MAXIMO = 500;

    public Mensagem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(operacaoId, "operacaoId");
        Objects.requireNonNull(autorId, "autorId");
        Objects.requireNonNull(enviadaEm, "enviadaEm");
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O texto da mensagem é obrigatório.");
        }
        if (texto.length() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException(
                    "Mensagem com mais de " + TAMANHO_MAXIMO + " caracteres.");
        }
    }
}
