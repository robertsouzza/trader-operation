package com.traderoperation.chat.application.port.out;

import com.traderoperation.chat.domain.Mensagem;
import java.util.List;
import java.util.UUID;

public interface MensagemRepositoryPort {

    void salvar(Mensagem mensagem);

    List<Mensagem> listarMaisRecentes(UUID operacaoId, int limite);
}
