package br.monitoriamap.relatorio;

import br.monitoriamap.config.AppProperties;
import br.monitoriamap.solicitacao.Modalidade;
import br.monitoriamap.solicitacao.Solicitacao;
import br.monitoriamap.solicitacao.SolicitacaoRepository;
import br.monitoriamap.solicitacao.StatusSolicitacao;
import br.monitoriamap.topico.Topico;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static br.monitoriamap.Fixtures.INICIO;
import static br.monitoriamap.Fixtures.solicitacao;
import static br.monitoriamap.Fixtures.topico;
import static br.monitoriamap.Fixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;

class RelatorioServiceTest {

    private final RelatorioService servico = new RelatorioService(Mockito.mock(SolicitacaoRepository.class),
            new AppProperties(null, null, null, null, null, "http://localhost", ZoneId.of("America/Fortaleza")));

    private final Usuario aluno = usuario(1, "Maria", Papel.ALUNO);
    private final Usuario ana = usuario(2, "Ana", Papel.MONITOR);
    private final Usuario bruno = usuario(3, "Bruno", Papel.MONITOR);
    private final Topico observer = topico(1, "Observer");
    private final Topico testes = topico(2, "Testes");

    private static Instant mais(int minutos) {
        return INICIO.plus(Duration.ofMinutes(minutos));
    }

    @Test
    void calculaTemposMediosEDesempenhoPorMonitor() {
        Solicitacao s1 = solicitacao(aluno, observer, INICIO);
        s1.assumir(ana, mais(10));        // espera 10
        s1.concluir(ana, null, mais(40)); // atendimento 30

        Solicitacao s2 = solicitacao(aluno, observer, INICIO);
        s2.assumir(ana, mais(20));        // espera 20
        s2.concluir(ana, null, mais(30)); // atendimento 10

        Solicitacao s3 = solicitacao(aluno, testes, INICIO);
        s3.assumir(bruno, mais(30));      // espera 30, ainda em atendimento

        Solicitacao s4 = Solicitacao.abrir(aluno, testes, "t", "d", Modalidade.PRESENCIAL, null, INICIO);
        s4.cancelar(aluno, mais(1));

        LocalDate dia = LocalDate.of(2026, 10, 14);
        ResumoRelatorio resumo = servico.resumir(dia, dia, List.of(s1, s2, s3, s4));

        assertThat(resumo.total()).isEqualTo(4);
        assertThat(resumo.porStatus())
                .containsEntry(StatusSolicitacao.CONCLUIDA, 2L)
                .containsEntry(StatusSolicitacao.EM_ATENDIMENTO, 1L)
                .containsEntry(StatusSolicitacao.CANCELADA, 1L)
                .containsEntry(StatusSolicitacao.ABERTA, 0L);
        assertThat(resumo.tempoMedioEsperaMinutos()).isEqualTo(20.0);
        assertThat(resumo.tempoMedioAtendimentoMinutos()).isEqualTo(20.0);
        assertThat(resumo.porMonitor()).singleElement().satisfies(m -> {
            assertThat(m.nome()).isEqualTo("Ana");
            assertThat(m.atendimentosConcluidos()).isEqualTo(2);
        });
        assertThat(resumo.porTopico()).extracting(ResumoRelatorio.Contagem::rotulo).containsExactly("Observer", "Testes");
        assertThat(resumo.porModalidade()).containsEntry(Modalidade.ONLINE, 3L).containsEntry(Modalidade.PRESENCIAL, 1L);
        assertThat(resumo.porSemana()).containsExactly(new ResumoRelatorio.Contagem("2026-10-12", 4));
    }

    @Test
    void periodoVazioNaoTemMedias() {
        LocalDate dia = LocalDate.of(2026, 10, 14);
        ResumoRelatorio resumo = servico.resumir(dia, dia, List.of());

        assertThat(resumo.total()).isZero();
        assertThat(resumo.tempoMedioEsperaMinutos()).isNull();
        assertThat(resumo.porMonitor()).isEmpty();
    }
}
