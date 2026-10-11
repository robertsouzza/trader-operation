package com.traderoperation.tempo_real.application.port.in;

import java.util.UUID;

public interface ContadorParticipantesQuery {

    long contar(UUID operacaoId);

    long entrar(UUID operacaoId, String sessaoId);

    long sair(UUID operacaoId, String sessaoId);
}
