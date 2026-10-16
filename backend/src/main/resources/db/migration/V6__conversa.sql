-- Conversa dentro do pedido, entre o aluno e o monitor responsável.
-- As mensagens ficam numa tabela própria (e não no histórico): o histórico registra o que
-- aconteceu com o pedido; a conversa é o que as pessoas disseram uma à outra.
CREATE TABLE mensagem_solicitacao (
    id             BIGSERIAL    PRIMARY KEY,
    solicitacao_id BIGINT       NOT NULL REFERENCES solicitacao (id) ON DELETE CASCADE,
    autor_id       BIGINT       NOT NULL REFERENCES usuario (id),
    texto          TEXT         NOT NULL CHECK (char_length(texto) BETWEEN 1 AND 4000),
    enviada_em     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_mensagem_solicitacao ON mensagem_solicitacao (solicitacao_id, enviada_em, id);
