package br.monitoriamap.solicitacao;

import br.monitoriamap.common.AcessoNegadoException;
import br.monitoriamap.usuario.Usuario;

import java.time.Instant;

/** Um monitor está cuidando do pedido: só ele (ou um admin) pode concluir ou devolver. */
final class EstadoEmAtendimento implements EstadoSolicitacao {

    @Override
    public StatusSolicitacao status() {
        return StatusSolicitacao.EM_ATENDIMENTO;
    }

    @Override
    public void assumir(Solicitacao solicitacao, Usuario monitor, Instant agora) {
        throw new TransicaoInvalidaException(
                "Esta solicitação já foi assumida por " + solicitacao.getMonitor().getNome() + ".");
    }

    @Override
    public void concluir(Solicitacao solicitacao, Usuario usuario, String observacao, Instant agora) {
        exigirResponsavel(solicitacao, usuario);
        solicitacao.registrarConclusao(usuario, observacao, agora);
    }

    @Override
    public void devolver(Solicitacao solicitacao, Usuario usuario, Instant agora) {
        exigirResponsavel(solicitacao, usuario);
        solicitacao.registrarDevolucao(usuario, agora);
    }

    @Override
    public void cancelar(Solicitacao solicitacao, Usuario usuario, Instant agora) {
        throw new TransicaoInvalidaException(
                "A solicitação já está em atendimento. Fale com o monitor "
                        + solicitacao.getMonitor().getNome() + ".");
    }

    /** Durante o atendimento, o aluno e o monitor responsável podem pôr ou trocar o link. */
    @Override
    public void definirLink(Solicitacao solicitacao, Usuario usuario, String link, Instant agora) {
        if (!solicitacao.foiAbertaPor(usuario)) {
            exigirResponsavel(solicitacao, usuario);
        }
        solicitacao.registrarLink(usuario, link, agora);
    }

    private static void exigirResponsavel(Solicitacao solicitacao, Usuario usuario) {
        if (!solicitacao.estaSendoAtendidaPor(usuario) && !usuario.isAdmin()) {
            throw new AcessoNegadoException("Só o monitor responsável pode alterar este atendimento.");
        }
    }
}
