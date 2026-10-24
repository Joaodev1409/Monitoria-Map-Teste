package br.monitoriamap.solicitacao;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status persistido no banco. Cada constante carrega o objeto de estado (padrão State)
 * que sabe quais transições são permitidas a partir dela.
 */
public enum StatusSolicitacao {
    ABERTA("aberta", new EstadoAberta()),
    EM_ATENDIMENTO("em atendimento", new EstadoEmAtendimento()),
    CONCLUIDA("concluída", new EstadoConcluida()),
    CANCELADA("cancelada", new EstadoCancelada());

    /** Ainda "vivas": na fila ou sendo atendidas. */
    public static final Set<StatusSolicitacao> ATIVOS = EnumSet.of(ABERTA, EM_ATENDIMENTO);

    private final String descricao;
    private final EstadoSolicitacao estado;

    StatusSolicitacao(String descricao, EstadoSolicitacao estado) {
        this.descricao = descricao;
        this.estado = estado;
    }

    public String descricao() {
        return descricao;
    }

    EstadoSolicitacao estado() {
        return estado;
    }
}
