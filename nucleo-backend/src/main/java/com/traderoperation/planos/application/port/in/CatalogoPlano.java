package com.traderoperation.planos.application.port.in;

import com.traderoperation.planos.domain.Direito;
import com.traderoperation.planos.domain.Plano;
import java.util.Set;

/** Linha do catálogo público de planos. */
public record CatalogoPlano(Plano plano, Set<Direito> direitos) {
}
