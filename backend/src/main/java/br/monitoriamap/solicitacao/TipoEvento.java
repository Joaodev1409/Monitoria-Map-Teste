package br.monitoriamap.solicitacao;

/** O que aconteceu com uma solicitação. Cada ocorrência vira uma linha no histórico. */
public enum TipoEvento {
    CRIADA,
    ASSUMIDA,
    DEVOLVIDA,
    CONCLUIDA,
    CANCELADA,
    /** O link da reunião online foi definido ou trocado. */
    LINK_DEFINIDO
}
