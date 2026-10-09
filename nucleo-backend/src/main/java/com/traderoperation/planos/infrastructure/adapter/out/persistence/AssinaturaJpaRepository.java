package com.traderoperation.planos.infrastructure.adapter.out.persistence;

import com.traderoperation.planos.domain.StatusAssinatura;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AssinaturaJpaRepository extends JpaRepository<AssinaturaJpaEntity, UUID> {

    Optional<AssinaturaJpaEntity> findByUsuarioIdAndStatus(UUID usuarioId, StatusAssinatura status);

    List<AssinaturaJpaEntity> findByUsuarioIdInAndStatus(Collection<UUID> usuarioIds, StatusAssinatura status);
}
