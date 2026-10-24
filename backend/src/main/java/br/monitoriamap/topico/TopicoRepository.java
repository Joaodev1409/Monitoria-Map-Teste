package br.monitoriamap.topico;

import br.monitoriamap.common.RecursoNaoEncontradoException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicoRepository extends JpaRepository<Topico, Long> {

    List<Topico> findAllByOrderByNomeAsc();

    List<Topico> findByAtivoTrueOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    /** O tópico com este id, ou 404. */
    default Topico buscar(Long id) {
        return findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Tópico", id));
    }
}
