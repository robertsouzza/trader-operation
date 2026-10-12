package com.traderoperation.chat.application.port.in;

import java.time.Instant;
import java.util.UUID;

public record MensagemResumo(
        UUID id,
        UUID operacaoId,
        UUID autorId,
        String autorNome,
        String texto,
        Instant enviadaEm) {
}
