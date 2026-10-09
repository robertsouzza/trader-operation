-- Assinaturas de plano (skill 02). Um usuário tem no máximo uma ATIVA por vez.
CREATE TABLE assinaturas (
    id         UUID        PRIMARY KEY,
    usuario_id UUID        NOT NULL REFERENCES usuarios(id),
    plano      VARCHAR(20) NOT NULL CHECK (plano  IN ('FREE', 'PRO', 'PREMIUM')),
    status     VARCHAR(20) NOT NULL CHECK (status IN ('ATIVA', 'CANCELADA', 'VENCIDA')),
    origem     VARCHAR(20) NOT NULL CHECK (origem IN ('MANUAL', 'PAGAMENTO')),
    inicio_em  TIMESTAMPTZ NOT NULL,
    fim_em     TIMESTAMPTZ,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_assinaturas_ativa_por_usuario
    ON assinaturas (usuario_id) WHERE status = 'ATIVA';
CREATE INDEX ix_assinaturas_usuario ON assinaturas (usuario_id);
