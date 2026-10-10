package com.traderoperation.operacoes.application.port.in;

import com.traderoperation.operacoes.domain.Direcao;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicarOperacaoCmd(
        UUID masterId,
        String ativo,
        Direcao direcao,
        BigDecimal entrada,
        BigDecimal stop,
        List<BigDecimal> alvos,
        String estrategia) {
}
