package com.traderoperation.chat.application.port.in;

import java.util.UUID;

public record EnviarMensagemCmd(
        UUID operacaoId,
        UUID autorId,
        String texto) {
}
