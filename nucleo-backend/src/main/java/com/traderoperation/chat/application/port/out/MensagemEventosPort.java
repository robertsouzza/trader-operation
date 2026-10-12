package com.traderoperation.chat.application.port.out;

import com.traderoperation.chat.domain.Mensagem;

/**
 * Porta de eventos de chat. No Bloco A o adapter é no-op; no Bloco B o adapter
 * publica em Redis para broadcast via STOMP.
 */
public interface MensagemEventosPort {

    void aoEnviar(Mensagem mensagem);
}
