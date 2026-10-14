-- Login próprio com e-mail institucional + senha, confirmado por código enviado ao e-mail.
ALTER TABLE usuario ADD COLUMN senha_hash VARCHAR(100);
ALTER TABLE usuario ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE;
-- Quem já existia entrou pelo Google (e-mail verificado por ele) ou pelo antigo login de desenvolvimento.
UPDATE usuario SET email_verificado = TRUE;

CREATE TABLE codigo_verificacao (
    id           BIGSERIAL PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    finalidade   VARCHAR(20)  NOT NULL CHECK (finalidade IN ('CADASTRO', 'REDEFINIR_SENHA')),
    codigo_hash  VARCHAR(100) NOT NULL,  -- o código em si nunca é guardado, só o hash
    criado_em    TIMESTAMPTZ  NOT NULL,
    expira_em    TIMESTAMPTZ  NOT NULL,
    tentativas   INT          NOT NULL DEFAULT 0,
    usado        BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_codigo_usuario ON codigo_verificacao (usuario_id, finalidade, criado_em DESC);

-- Novo evento no histórico: o link da reunião foi definido ou trocado.
ALTER TABLE evento_solicitacao DROP CONSTRAINT evento_solicitacao_tipo_check;
ALTER TABLE evento_solicitacao ADD CONSTRAINT evento_solicitacao_tipo_check
    CHECK (tipo IN ('CRIADA', 'ASSUMIDA', 'DEVOLVIDA', 'CONCLUIDA', 'CANCELADA', 'LINK_DEFINIDO'));
