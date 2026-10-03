package com.traderoperation.auditoria.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RegistroAuditoriaJpaRepository extends JpaRepository<RegistroAuditoriaJpaEntity, UUID> {

    long countByAcao(String acao);
}
