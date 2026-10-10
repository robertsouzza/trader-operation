-- Operações publicadas pelo master (D-02, D-27). Imutáveis no histórico — mudanças
-- de status (PUBLICADA → ENCERRADA) gravam no mesmo registro, com resultado e observação.
CREATE TABLE operacoes (
    id            UUID        PRIMARY KEY,
    ativo         VARCHAR(20) NOT NULL,
    direcao       VARCHAR(10) NOT NULL CHECK (direcao IN ('COMPRA', 'VENDA')),
    entrada       NUMERIC(18, 8) NOT NULL CHECK (entrada > 0),
    stop          NUMERIC(18, 8) NOT NULL CHECK (stop > 0),
    alvos         NUMERIC(18, 8)[] NOT NULL CHECK (array_length(alvos, 1) >= 1),
    estrategia    VARCHAR(120),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('PUBLICADA', 'ENCERRADA')),
    resultado     VARCHAR(20)       CHECK (resultado IN ('GAIN', 'LOSS', 'NEUTRO', 'INDEFINIDO')),
    observacao    TEXT,
    master_id     UUID        NOT NULL REFERENCES usuarios(id),
    publicada_em  TIMESTAMPTZ NOT NULL,
    encerrada_em  TIMESTAMPTZ,
    CONSTRAINT ck_operacoes_encerrada_coerente
        CHECK ((status = 'ENCERRADA' AND encerrada_em IS NOT NULL AND resultado IS NOT NULL)
            OR (status = 'PUBLICADA' AND encerrada_em IS NULL AND resultado IS NULL))
);

CREATE INDEX ix_operacoes_status ON operacoes (status);
CREATE INDEX ix_operacoes_publicada_em ON operacoes (publicada_em DESC);
CREATE INDEX ix_operacoes_master ON operacoes (master_id);
