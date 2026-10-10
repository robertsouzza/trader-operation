package com.traderoperation.operacoes.infrastructure.adapter.out.eventos;

import com.traderoperation.operacoes.application.port.out.OperacaoEventosPort;
import com.traderoperation.operacoes.domain.Operacao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Adapter no-op: até o Bloco B registrar o adapter de Redis, as publicações de
 * operações ficam só na transação (persistência) sem broadcast ao vivo.
 */
@Component
@ConditionalOnMissingBean(name = "operacaoEventosRedisAdapter")
class OperacaoEventosNoop implements OperacaoEventosPort {

    @Override
    public void aoPublicar(Operacao operacao) {
        // Bloco A: nenhum broadcast ainda.
    }

    @Override
    public void aoEncerrar(Operacao operacao) {
        // Bloco A: nenhum broadcast ainda.
    }
}
