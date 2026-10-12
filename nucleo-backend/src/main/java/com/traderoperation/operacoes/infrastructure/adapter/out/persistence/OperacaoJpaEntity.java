package com.traderoperation.operacoes.infrastructure.adapter.out.persistence;

import com.traderoperation.operacoes.domain.Direcao;
import com.traderoperation.operacoes.domain.ResultadoOperacao;
import com.traderoperation.operacoes.domain.StatusOperacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "operacoes")
@Getter
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OperacaoJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String ativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direcao direcao;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal entrada;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal stop;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "numeric(18,8)[]")
    private BigDecimal[] alvos;

    @Column(length = 120)
    private String estrategia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOperacao status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResultadoOperacao resultado;

    @Column(columnDefinition = "text")
    private String observacao;

    @Column(name = "master_id", nullable = false)
    private UUID masterId;

    @Column(name = "publicada_em", nullable = false)
    private Instant publicadaEm;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;
}
