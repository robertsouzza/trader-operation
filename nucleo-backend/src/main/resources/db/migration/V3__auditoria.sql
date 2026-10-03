-- Diário de auditoria (RF-14). Só recebe INSERT; nada é alterado ou apagado.
CREATE TABLE registros_auditoria (
    id          UUID        PRIMARY KEY,
    usuario_id  UUID,
    acao        VARCHAR(80) NOT NULL,
    entidade    VARCHAR(80),
    entidade_id VARCHAR(80),
    dados       JSONB       NOT NULL DEFAULT '{}'::jsonb,
    ocorrido_em TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_auditoria_usuario  ON registros_auditoria (usuario_id);
CREATE INDEX ix_auditoria_entidade ON registros_auditoria (entidade, entidade_id);
CREATE INDEX ix_auditoria_ocorrido ON registros_auditoria (ocorrido_em);
