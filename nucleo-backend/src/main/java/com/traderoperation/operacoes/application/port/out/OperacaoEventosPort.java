package com.traderoperation.operacoes.application.port.out;

import com.traderoperation.operacoes.domain.Operacao;

/**
 * Porta de saída de eventos de operações. No Bloco A o adapter é um no-op;
 * o Bloco B (tempo real) implementa um adapter que publica no Redis para
 * broadcast via STOMP.
 */
public interface OperacaoEventosPort {

    void aoPublicar(Operacao operacao);

    void aoEncerrar(Operacao operacao);
}
