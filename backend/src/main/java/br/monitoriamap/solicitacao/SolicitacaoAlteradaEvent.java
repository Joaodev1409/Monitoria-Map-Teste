package br.monitoriamap.solicitacao;

import br.monitoriamap.solicitacao.dto.SolicitacaoDTO;

/**
 * Evento de domínio publicado sempre que uma solicitação muda (padrão Observer).
 * Carrega uma "foto" já convertida em DTO, para que os ouvintes, que rodam depois do
 * commit e em outra thread, não precisem acessar o banco nem entidades JPA.
 */
public record SolicitacaoAlteradaEvent(TipoEvento tipo, SolicitacaoDTO solicitacao) {
}
