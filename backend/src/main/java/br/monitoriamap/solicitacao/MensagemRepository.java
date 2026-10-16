package br.monitoriamap.solicitacao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensagemRepository extends JpaRepository<MensagemSolicitacao, Long> {

    @EntityGraph(attributePaths = "autor")
    List<MensagemSolicitacao> findBySolicitacaoIdOrderByEnviadaEmAscIdAsc(Long solicitacaoId);
}
