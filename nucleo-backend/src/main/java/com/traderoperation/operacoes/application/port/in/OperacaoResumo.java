package com.traderoperation.operacoes.application.port.in;

import com.traderoperation.operacoes.domain.Direcao;
import com.traderoperation.operacoes.domain.ResultadoOperacao;
import com.traderoperation.operacoes.domain.StatusOperacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Representação pronta para serializar na API, sem expor entidades JPA. */
public record OperacaoResumo(
        UUID id,
        String ativo,
        Direcao direcao,
        BigDecimal entrada,
        BigDecimal stop,
        List<BigDecimal> alvos,
        String estrategia,
        StatusOperacao status,
        ResultadoOperacao resultado,
        String observacao,
        UUID masterId,
        String masterNome,
        Instant publicadaEm,
        Instant encerradaEm) {
}
