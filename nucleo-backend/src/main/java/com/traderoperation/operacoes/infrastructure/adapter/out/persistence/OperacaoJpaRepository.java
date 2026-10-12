package com.traderoperation.operacoes.infrastructure.adapter.out.persistence;

import com.traderoperation.operacoes.domain.StatusOperacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OperacaoJpaRepository extends JpaRepository<OperacaoJpaEntity, UUID> {

    List<OperacaoJpaEntity> findByStatusAndPublicadaEmLessThanEqualOrderByPublicadaEmDesc(
            StatusOperacao status, Instant publicadaEmMaximo);
}
