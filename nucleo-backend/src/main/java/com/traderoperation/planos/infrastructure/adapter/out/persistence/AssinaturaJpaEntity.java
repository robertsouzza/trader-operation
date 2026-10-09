package com.traderoperation.planos.infrastructure.adapter.out.persistence;

import com.traderoperation.planos.domain.OrigemAssinatura;
import com.traderoperation.planos.domain.Plano;
import com.traderoperation.planos.domain.StatusAssinatura;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assinaturas")
@Getter
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AssinaturaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plano plano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAssinatura status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemAssinatura origem;

    @Column(name = "inicio_em", nullable = false)
    private Instant inicioEm;

    @Column(name = "fim_em")
    private Instant fimEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;
}
