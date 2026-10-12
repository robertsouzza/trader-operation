package com.traderoperation.operacoes.application.port.in;

import com.traderoperation.operacoes.domain.ResultadoOperacao;
import java.util.UUID;

public record EncerrarOperacaoCmd(
        UUID operacaoId,
        UUID masterId,
        ResultadoOperacao resultado,
        String observacao) {
}
