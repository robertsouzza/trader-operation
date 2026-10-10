package com.traderoperation.chat.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_mensagens")
@Getter
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class MensagemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "operacao_id", nullable = false)
    private UUID operacaoId;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(name = "enviada_em", nullable = false)
    private Instant enviadaEm;
}
