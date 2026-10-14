package br.monitoriamap.solicitacao.dto;

import br.monitoriamap.solicitacao.EventoSolicitacao;
import br.monitoriamap.solicitacao.Modalidade;
import br.monitoriamap.solicitacao.Solicitacao;
import br.monitoriamap.solicitacao.StatusSolicitacao;
import br.monitoriamap.solicitacao.TipoEvento;
import br.monitoriamap.usuario.UsuarioResumoDTO;

import java.time.Instant;
import java.util.List;

/** O que a API devolve sobre uma solicitação. Nunca expomos a entidade JPA diretamente. */
public record SolicitacaoDTO(
        Long id,
        String titulo,
        String descricao,
        TopicoResumoDTO topico,
        Modalidade modalidade,
        String linkReuniao,
        StatusSolicitacao status,
        UsuarioResumoDTO aluno,
        UsuarioResumoDTO monitor,
        Instant criadaEm,
        Instant assumidaEm,
        Instant concluidaEm,
        Instant canceladaEm,
        String observacaoConclusao,
        List<EventoDTO> historico
) {

    public record TopicoResumoDTO(Long id, String nome) {
    }

    public record EventoDTO(TipoEvento tipo, UsuarioResumoDTO autor, Instant ocorridoEm, String detalhe) {

        static EventoDTO de(EventoSolicitacao evento) {
            return new EventoDTO(evento.getTipo(), UsuarioResumoDTO.de(evento.getAutor()),
                    evento.getOcorridoEm(), evento.getDetalhe());
        }
    }

    /** Versão para listas: sem o histórico, que custaria uma consulta extra por item. */
    public static SolicitacaoDTO resumo(Solicitacao s) {
        return de(s, List.of());
    }

    public static SolicitacaoDTO detalhe(Solicitacao s) {
        return de(s, s.getEventos().stream().map(EventoDTO::de).toList());
    }

    private static SolicitacaoDTO de(Solicitacao s, List<EventoDTO> historico) {
        return new SolicitacaoDTO(
                s.getId(),
                s.getTitulo(),
                s.getDescricao(),
                new TopicoResumoDTO(s.getTopico().getId(), s.getTopico().getNome()),
                s.getModalidade(),
                s.getLinkReuniao(),
                s.getStatus(),
                UsuarioResumoDTO.de(s.getAluno()),
                UsuarioResumoDTO.de(s.getMonitor()),
                s.getCriadaEm(),
                s.getAssumidaEm(),
                s.getConcluidaEm(),
                s.getCanceladaEm(),
                s.getObservacaoConclusao(),
                historico);
    }
}
