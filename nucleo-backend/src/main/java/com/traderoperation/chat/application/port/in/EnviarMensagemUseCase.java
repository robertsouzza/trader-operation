package com.traderoperation.chat.application.port.in;

public interface EnviarMensagemUseCase {

    MensagemResumo enviar(EnviarMensagemCmd cmd);
}
