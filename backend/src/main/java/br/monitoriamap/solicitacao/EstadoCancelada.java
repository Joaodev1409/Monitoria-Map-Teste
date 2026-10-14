package br.monitoriamap.solicitacao;

/** Estado final: nenhuma ação é permitida, então nada é sobrescrito. */
final class EstadoCancelada implements EstadoSolicitacao {

    @Override
    public StatusSolicitacao status() {
        return StatusSolicitacao.CANCELADA;
    }
}
