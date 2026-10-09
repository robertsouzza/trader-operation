package com.traderoperation.planos.application.port.in;

import java.util.UUID;

public interface ConsultarPlanoDoUsuarioQuery {

    /** Retorna o plano atual do usuário. Se não houver assinatura ativa, assume {@code FREE}. */
    PlanoAtual consultar(UUID usuarioId);
}
