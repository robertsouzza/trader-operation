package com.traderoperation.planos.application.port.out;

import com.traderoperation.planos.domain.Assinatura;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AssinaturaRepositoryPort {

    Optional<Assinatura> buscarAtivaDoUsuario(UUID usuarioId);

    /** Assinaturas ATIVAS dos usuários pedidos, chaveadas por {@code usuarioId}. */
    Map<UUID, Assinatura> buscarAtivasDeVarios(Set<UUID> usuarioIds);

    void salvar(Assinatura assinatura);

    /** Reflete uma atualização de status/fimEm feita no agregado. */
    void atualizar(Assinatura assinatura);
}
