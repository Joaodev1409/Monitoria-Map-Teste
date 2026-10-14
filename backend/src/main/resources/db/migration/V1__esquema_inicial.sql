-- Esquema inicial da Monitoria MAP.
-- Migrações do Flyway nunca são editadas depois de aplicadas: mudanças futuras vão em V2, V3...

CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    foto_url    VARCHAR(1000),
    papel       VARCHAR(20)  NOT NULL CHECK (papel IN ('ALUNO', 'MONITOR', 'ADMIN')),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    telefone    VARCHAR(30),
    criado_em   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE topico (
    id      BIGSERIAL PRIMARY KEY,
    nome    VARCHAR(80) NOT NULL UNIQUE,
    ativo   BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE solicitacao (
    id                    BIGSERIAL PRIMARY KEY,
    aluno_id              BIGINT       NOT NULL REFERENCES usuario (id),
    monitor_id            BIGINT       REFERENCES usuario (id),
    topico_id             BIGINT       NOT NULL REFERENCES topico (id),
    titulo                VARCHAR(120) NOT NULL,
    descricao             TEXT         NOT NULL,
    modalidade            VARCHAR(20)  NOT NULL CHECK (modalidade IN ('PRESENCIAL', 'ONLINE')),
    link_reuniao          VARCHAR(500),
    status                VARCHAR(20)  NOT NULL
                          CHECK (status IN ('ABERTA', 'EM_ATENDIMENTO', 'CONCLUIDA', 'CANCELADA')),
    criada_em             TIMESTAMPTZ  NOT NULL,
    assumida_em           TIMESTAMPTZ,
    concluida_em          TIMESTAMPTZ,
    cancelada_em          TIMESTAMPTZ,
    observacao_conclusao  TEXT,
    versao                BIGINT       NOT NULL DEFAULT 0,
    -- Regra de integridade: estar em atendimento ou concluída exige um monitor.
    CONSTRAINT monitor_obrigatorio CHECK (status IN ('ABERTA', 'CANCELADA') OR monitor_id IS NOT NULL)
);

CREATE INDEX idx_solicitacao_status_criada ON solicitacao (status, criada_em);
CREATE INDEX idx_solicitacao_aluno ON solicitacao (aluno_id, criada_em DESC);
CREATE INDEX idx_solicitacao_monitor ON solicitacao (monitor_id, assumida_em DESC);
CREATE INDEX idx_solicitacao_criada ON solicitacao (criada_em);

CREATE TABLE evento_solicitacao (
    id              BIGSERIAL PRIMARY KEY,
    solicitacao_id  BIGINT       NOT NULL REFERENCES solicitacao (id) ON DELETE CASCADE,
    tipo            VARCHAR(20)  NOT NULL
                    CHECK (tipo IN ('CRIADA', 'ASSUMIDA', 'DEVOLVIDA', 'CONCLUIDA', 'CANCELADA')),
    autor_id        BIGINT       NOT NULL REFERENCES usuario (id),
    ocorrido_em     TIMESTAMPTZ  NOT NULL,
    detalhe         TEXT
);

CREATE INDEX idx_evento_solicitacao ON evento_solicitacao (solicitacao_id, ocorrido_em);
