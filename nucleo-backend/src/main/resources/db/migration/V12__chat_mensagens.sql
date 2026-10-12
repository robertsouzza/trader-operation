-- Chat por operação (RF-07). Mensagens persistem para histórico/relatório
-- mesmo após a operação ser encerrada (D-26).
CREATE TABLE chat_mensagens (
    id          UUID         PRIMARY KEY,
    operacao_id UUID         NOT NULL REFERENCES operacoes(id) ON DELETE CASCADE,
    autor_id    UUID         NOT NULL REFERENCES usuarios(id),
    texto       VARCHAR(500) NOT NULL,
    enviada_em  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_chat_operacao_enviada ON chat_mensagens (operacao_id, enviada_em DESC);
CREATE INDEX ix_chat_autor ON chat_mensagens (autor_id);
