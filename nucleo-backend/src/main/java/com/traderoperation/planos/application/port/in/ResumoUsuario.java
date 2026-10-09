package com.traderoperation.planos.application.port.in;

import com.traderoperation.planos.domain.Plano;
import java.time.Instant;
import java.util.UUID;

/** Linha da listagem administrativa de usuários. */
public record ResumoUsuario(
        UUID id,
        String nome,
        String email,
        String perfil,
        Plano planoAtual,
        Instant assinaturaAtivaFimEm) {
}
