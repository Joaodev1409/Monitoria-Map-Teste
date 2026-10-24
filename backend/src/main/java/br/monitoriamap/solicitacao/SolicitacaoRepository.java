package br.monitoriamap.solicitacao;

import br.monitoriamap.common.RecursoNaoEncontradoException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Os {@code @EntityGraph} trazem aluno, monitor e tópico na mesma consulta,
 * evitando o problema N+1 ao montar as listas.
 */
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    @EntityGraph(attributePaths = {"aluno", "monitor", "topico"})
    List<Solicitacao> findByAlunoIdOrderByCriadaEmDesc(Long alunoId);

    @EntityGraph(attributePaths = {"aluno", "monitor", "topico"})
    List<Solicitacao> findByStatusOrderByCriadaEmAsc(StatusSolicitacao status);

    @EntityGraph(attributePaths = {"aluno", "monitor", "topico"})
    List<Solicitacao> findByMonitorIdOrderByAssumidaEmDesc(Long monitorId);

    @EntityGraph(attributePaths = {"aluno", "monitor", "topico"})
    List<Solicitacao> findByCriadaEmBetweenOrderByCriadaEmAsc(Instant inicio, Instant fim);

    @EntityGraph(attributePaths = {"aluno", "monitor", "topico", "eventos", "eventos.autor"})
    Optional<Solicitacao> findDetalhadaById(Long id);

    long countByStatus(StatusSolicitacao status);

    long countByAlunoIdAndStatusIn(Long alunoId, Collection<StatusSolicitacao> status);

    /** A solicitação com este id, ou 404. */
    default Solicitacao buscar(Long id) {
        return findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação", id));
    }

    /** A solicitação com aluno, monitor, tópico e histórico já carregados, ou 404. */
    default Solicitacao buscarDetalhada(Long id) {
        return findDetalhadaById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação", id));
    }
}
