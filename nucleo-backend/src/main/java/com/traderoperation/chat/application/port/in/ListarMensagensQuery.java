package com.traderoperation.chat.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ListarMensagensQuery {

    List<MensagemResumo> listar(UUID operacaoId, int limite);
}
