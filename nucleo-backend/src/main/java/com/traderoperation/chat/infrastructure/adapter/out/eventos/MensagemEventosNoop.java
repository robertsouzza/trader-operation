package com.traderoperation.chat.infrastructure.adapter.out.eventos;

import com.traderoperation.chat.application.port.out.MensagemEventosPort;
import com.traderoperation.chat.domain.Mensagem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Bloco A: sem broadcast. Bloco B adiciona o adapter de Redis que publica para STOMP. */
@Component
@ConditionalOnMissingBean(name = "mensagemEventosRedisAdapter")
class MensagemEventosNoop implements MensagemEventosPort {

    @Override
    public void aoEnviar(Mensagem mensagem) {
        // sem efeito
    }
}
