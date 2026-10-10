package com.traderoperation.chat.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface MensagemJpaRepository extends JpaRepository<MensagemJpaEntity, UUID> {

    @Query("SELECT m FROM MensagemJpaEntity m WHERE m.operacaoId = :operacaoId ORDER BY m.enviadaEm DESC")
    java.util.List<MensagemJpaEntity> listarMaisRecentes(UUID operacaoId, Pageable pageable);
}
