package br.monitoriamap.topico;

import br.monitoriamap.common.RecursoNaoEncontradoException;
import br.monitoriamap.common.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static br.monitoriamap.Fixtures.topico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopicoServiceTest {

    // CALLS_REAL_METHODS: o dublê executa os métodos default do repositório (ex.: buscar),
    // e só as consultas do Spring Data (abstratas) ficam a cargo do teste.
    private final TopicoRepository topicos = mock(TopicoRepository.class, CALLS_REAL_METHODS);
    private final TopicoService servico = new TopicoService(topicos);

    @Test
    void criaTopicoSemEspacosSobrando() {
        when(topicos.save(any(Topico.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(servico.criar("  Lista 3  ").nome()).isEqualTo("Lista 3");
    }

    @Test
    void naoCriaDoisTopicosComOMesmoNome() {
        when(topicos.existsByNomeIgnoreCase("Lista 3")).thenReturn(true);

        assertThatThrownBy(() -> servico.criar(" Lista 3 "))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um tópico chamado \"Lista 3\".");
    }

    @Test
    void renomearParaUmNomeQueJaExisteEhRecusado() {
        when(topicos.findById(1L)).thenReturn(Optional.of(topico(1, "Observer")));
        when(topicos.existsByNomeIgnoreCaseAndIdNot("Strategy", 1L)).thenReturn(true);

        assertThatThrownBy(() -> servico.atualizar(1L, "Strategy", null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um tópico chamado \"Strategy\".");
    }

    @Test
    void desativarSemMexerNoNome() {
        when(topicos.findById(1L)).thenReturn(Optional.of(topico(1, "Observer")));

        TopicoService.TopicoDTO atualizado = servico.atualizar(1L, null, false);

        assertThat(atualizado.nome()).isEqualTo("Observer");
        assertThat(atualizado.ativo()).isFalse();
    }

    @Test
    void topicoInexistenteDa404() {
        when(topicos.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.atualizar(9L, "x", null)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
