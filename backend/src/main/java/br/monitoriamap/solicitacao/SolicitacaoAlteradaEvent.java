package br.monitoriamap.solicitacao;

import br.monitoriamap.solicitacao.dto.SolicitacaoDTO;
import br.monitoriamap.usuario.UsuarioResumoDTO;

/**
 * Evento de domínio publicado sempre que uma solicitação muda (padrão Observer).
 * Carrega uma "foto" já convertida em DTO, para que os ouvintes, que rodam depois do
 * commit e em outra thread, não precisem acessar o banco nem entidades JPA.
 *
 * @param autor quem fez a mudança (ex.: o monitor que assumiu ou quem mandou o link)
 */
public record SolicitacaoAlteradaEvent(TipoEvento tipo, SolicitacaoDTO solicitacao, UsuarioResumoDTO autor) {
}
