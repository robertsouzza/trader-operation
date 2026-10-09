package com.traderoperation.planos.application.port.in;

import com.traderoperation.planos.domain.OrigemAssinatura;
import com.traderoperation.planos.domain.Plano;
import java.time.Duration;
import java.util.UUID;

public interface AtribuirPlanoUseCase {

    /**
     * Cancela a assinatura ativa atual (se houver) e cria uma nova para o usuário.
     * {@code duracao} nula = não expira (apropriado para FREE).
     */
    void atribuir(UUID usuarioId, Plano plano, OrigemAssinatura origem, Duration duracao);
}
