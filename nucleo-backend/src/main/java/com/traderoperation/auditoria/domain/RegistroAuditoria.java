package com.traderoperation.auditoria.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma linha do diário de auditoria (RF-14). Imutável: o diário só cresce.
 * {@code usuarioId} é nulo quando não há usuário identificado (ex.: login que falhou).
 */
public record RegistroAuditoria(
        UUID id,
        UUID usuarioId,
        String acao,
        String entidade,
        String entidadeId,
        Map<String, Object> dados,
        Instant ocorridoEm) {

    public RegistroAuditoria {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ocorridoEm, "ocorridoEm");
        if (acao == null || acao.isBlank()) {
            throw new IllegalArgumentException("A ação do registro de auditoria é obrigatória.");
        }
        dados = dados == null ? Map.of() : Map.copyOf(dados);
    }

    public static RegistroAuditoria novo(UUID usuarioId, String acao, String entidade, String entidadeId,
                                         Map<String, Object> dados, Instant agora) {
        return new RegistroAuditoria(UUID.randomUUID(), usuarioId, acao, entidade, entidadeId, dados, agora);
    }
}
