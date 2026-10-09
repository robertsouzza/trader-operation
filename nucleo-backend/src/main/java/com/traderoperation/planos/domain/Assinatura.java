package com.traderoperation.planos.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Assinatura de plano de um usuário (D-16). Imutável: toda troca cria uma nova linha
 * e cancela a anterior; nunca reaproveita o id. {@code fimEm} nulo = não expira (típico de FREE).
 */
public record Assinatura(
        UUID id,
        UUID usuarioId,
        Plano plano,
        StatusAssinatura status,
        OrigemAssinatura origem,
        Instant inicioEm,
        Instant fimEm) {

    public Assinatura {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(plano, "plano");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(origem, "origem");
        Objects.requireNonNull(inicioEm, "inicioEm");
        if (fimEm != null && fimEm.isBefore(inicioEm)) {
            throw new IllegalArgumentException("fimEm não pode ser anterior a inicioEm.");
        }
    }

    public Assinatura cancelar(Instant quando) {
        Objects.requireNonNull(quando, "quando");
        return new Assinatura(id, usuarioId, plano, StatusAssinatura.CANCELADA, origem, inicioEm, quando);
    }
}
