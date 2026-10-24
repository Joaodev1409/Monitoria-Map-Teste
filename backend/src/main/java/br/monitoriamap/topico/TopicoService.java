package br.monitoriamap.topico;

import br.monitoriamap.common.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TopicoService {

    private final TopicoRepository topicos;

    public TopicoService(TopicoRepository topicos) {
        this.topicos = topicos;
    }

    @Transactional(readOnly = true)
    public List<TopicoDTO> listar(boolean incluirInativos) {
        List<Topico> lista = incluirInativos ? topicos.findAllByOrderByNomeAsc() : topicos.findByAtivoTrueOrderByNomeAsc();
        return lista.stream().map(TopicoDTO::de).toList();
    }

    public TopicoDTO criar(String nome) {
        exigirNomeLivre(nome.trim(), topicos.existsByNomeIgnoreCase(nome.trim()));
        return TopicoDTO.de(topicos.save(new Topico(nome)));
    }

    public TopicoDTO atualizar(Long id, String nome, Boolean ativo) {
        Topico topico = topicos.buscar(id);
        if (nome != null && !nome.isBlank()) {
            exigirNomeLivre(nome.trim(), topicos.existsByNomeIgnoreCaseAndIdNot(nome.trim(), id));
            topico.renomear(nome);
        }
        if (ativo != null) {
            topico.definirAtivo(ativo);
        }
        return TopicoDTO.de(topico);
    }

    private static void exigirNomeLivre(String nome, boolean jaExiste) {
        if (jaExiste) {
            throw new RegraNegocioException("Já existe um tópico chamado \"" + nome + "\".");
        }
    }

    public record TopicoDTO(Long id, String nome, boolean ativo) {
        static TopicoDTO de(Topico t) {
            return new TopicoDTO(t.getId(), t.getNome(), t.isAtivo());
        }
    }
}
