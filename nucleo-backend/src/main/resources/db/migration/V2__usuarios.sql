CREATE TABLE usuarios (
    id            UUID         PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    senha_hash    VARCHAR(100) NOT NULL,
    perfil        VARCHAR(20)  NOT NULL CHECK (perfil IN ('ADMIN', 'MASTER', 'CLIENTE')),
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_email_minusculo CHECK (email = lower(email))
);
