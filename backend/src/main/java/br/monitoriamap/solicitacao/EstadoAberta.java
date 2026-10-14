package br.monitoriamap.solicitacao;

import br.monitoriamap.common.AcessoNegadoException;
import br.monitoriamap.usuario.Usuario;

import java.time.Instant;

/** Aguardando na fila: pode ser assumida por um monitor ou cancelada pelo aluno. */
final class EstadoAberta implements EstadoSolicitacao {

    @Override
    public StatusSolicitacao status() {
        return StatusSolicitacao.ABERTA;
    }

    @Override
    public void assumir(Solicitacao solicitacao, Usuario monitor, Instant agora) {
        if (!monitor.podeAtender()) {
            throw new AcessoNegadoException("Apenas monitores podem assumir solicitações.");
        }
        if (solicitacao.foiAbertaPor(monitor)) {
            throw new AcessoNegadoException("Você não pode atender a sua própria solicitação.");
        }
        solicitacao.registrarAssuncao(monitor, agora);
    }

    @Override
    public void cancelar(Solicitacao solicitacao, Usuario usuario, Instant agora) {
        if (!solicitacao.foiAbertaPor(usuario) && !usuario.isAdmin()) {
            throw new AcessoNegadoException("Só quem abriu a solicitação pode cancelá-la.");
        }
        solicitacao.registrarCancelamento(usuario, agora);
    }

    /** Enquanto ninguém assumiu, só o próprio aluno (ou um admin) mexe no link. */
    @Override
    public void definirLink(Solicitacao solicitacao, Usuario usuario, String link, Instant agora) {
        if (!solicitacao.foiAbertaPor(usuario) && !usuario.isAdmin()) {
            throw new AcessoNegadoException("Só quem abriu a solicitação pode definir o link agora.");
        }
        solicitacao.registrarLink(usuario, link, agora);
    }
}
