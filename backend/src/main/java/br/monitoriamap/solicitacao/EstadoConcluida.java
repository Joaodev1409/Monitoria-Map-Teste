package br.monitoriamap.solicitacao;

/** Estado final: nenhuma ação é permitida, então nada é sobrescrito. */
final class EstadoConcluida implements EstadoSolicitacao {

    @Override
    public StatusSolicitacao status() {
        return StatusSolicitacao.CONCLUIDA;
    }
}
