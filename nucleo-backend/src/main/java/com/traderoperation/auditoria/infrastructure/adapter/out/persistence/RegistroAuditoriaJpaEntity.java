package com.traderoperation.auditoria.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "registros_auditoria")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RegistroAuditoriaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 80)
    private String acao;

    @Column(length = 80)
    private String entidade;

    @Column(name = "entidade_id", length = 80)
    private String entidadeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> dados;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    RegistroAuditoriaJpaEntity(UUID id, UUID usuarioId, String acao, String entidade, String entidadeId,
                               Map<String, Object> dados, Instant ocorridoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.dados = dados;
        this.ocorridoEm = ocorridoEm;
    }
}
