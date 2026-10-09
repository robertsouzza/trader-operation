package com.traderoperation.planos.application.port.in;

import com.traderoperation.planos.domain.Direito;
import com.traderoperation.planos.domain.OrigemAssinatura;
import com.traderoperation.planos.domain.Plano;
import java.time.Instant;
import java.util.Set;

/**
 * Plano atual do usuário logado, pronto para serializar na API.
 * {@code direitos} é a união dos direitos vindos do perfil e do plano ativo.
 */
public record PlanoAtual(
        Plano plano,
        Instant inicioEm,
        Instant fimEm,
        OrigemAssinatura origem,
        Set<Direito> direitos) {
}
