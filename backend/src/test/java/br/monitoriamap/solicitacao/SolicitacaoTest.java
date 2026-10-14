package br.monitoriamap.solicitacao;

import br.monitoriamap.common.AcessoNegadoException;
import br.monitoriamap.topico.Topico;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static br.monitoriamap.Fixtures.INICIO;
import static br.monitoriamap.Fixtures.solicitacao;
import static br.monitoriamap.Fixtures.topico;
import static br.monitoriamap.Fixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testa o ciclo de vida (padrão State) sem banco, sem Spring: só o domínio. */
class SolicitacaoTest {

    private final Usuario aluno = usuario(1, "Maria Silva", Papel.ALUNO);
    private final Usuario ana = usuario(2, "Ana Monitora", Papel.MONITOR);
    private final Usuario bruno = usuario(3, "Bruno Monitor", Papel.MONITOR);
    private final Usuario admin = usuario(4, "Prof Admin", Papel.ADMIN);
    private final Topico observer = topico(1, "Padrões comportamentais");

    private Solicitacao solicitacao;

    @BeforeEach
    void abrir() {
        solicitacao = solicitacao(aluno, observer, INICIO);
    }

    private static Instant depois(int minutos) {
        return INICIO.plus(Duration.ofMinutes(minutos));
    }

    @Test
    @DisplayName("nasce aberta, sem monitor e com o evento CRIADA no histórico")
    void nasceAberta() {
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.ABERTA);
        assertThat(solicitacao.getMonitor()).isNull();
        assertThat(solicitacao.getEventos()).extracting(EventoSolicitacao::getTipo).containsExactly(TipoEvento.CRIADA);
    }

    @Test
    @DisplayName("descarta o link de reunião quando o atendimento é presencial")
    void presencialSemLink() {
        Solicitacao presencial = Solicitacao.abrir(aluno, observer, "t", "d", Modalidade.PRESENCIAL,
                "https://meet.google.com/x", INICIO);
        assertThat(presencial.getLinkReuniao()).isNull();
    }

    @Nested
    @DisplayName("quando ABERTA")
    class QuandoAberta {

        @Test
        void monitorAssume() {
            solicitacao.assumir(ana, depois(5));

            assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.EM_ATENDIMENTO);
            assertThat(solicitacao.getMonitor()).isEqualTo(ana);
            assertThat(solicitacao.tempoDeEspera()).contains(Duration.ofMinutes(5));
        }

        @Test
        void alunoNaoPodeAssumir() {
            Usuario outroAluno = usuario(9, "Joao", Papel.ALUNO);
            assertThatThrownBy(() -> solicitacao.assumir(outroAluno, depois(1)))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void monitorNaoAtendeAPropriaSolicitacao() {
            Solicitacao doMonitor = solicitacao(ana, observer, INICIO);
            assertThatThrownBy(() -> doMonitor.assumir(ana, depois(1)))
                    .isInstanceOf(AcessoNegadoException.class)
                    .hasMessageContaining("própria");
        }

        @Test
        void alunoCancela() {
            solicitacao.cancelar(aluno, depois(2));

            assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.CANCELADA);
            assertThat(solicitacao.getCanceladaEm()).isEqualTo(depois(2));
        }

        @Test
        void outroAlunoNaoCancela() {
            Usuario outroAluno = usuario(9, "Joao", Papel.ALUNO);
            assertThatThrownBy(() -> solicitacao.cancelar(outroAluno, depois(1)))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void naoPodeConcluirSemAntesAssumir() {
            assertThatThrownBy(() -> solicitacao.concluir(ana, null, depois(1)))
                    .isInstanceOf(TransicaoInvalidaException.class)
                    .hasMessage("Não é possível concluir uma solicitação aberta.");
        }
    }

    @Nested
    @DisplayName("quando EM_ATENDIMENTO")
    class QuandoEmAtendimento {

        @BeforeEach
        void assumir() {
            solicitacao.assumir(ana, depois(5));
        }

        @Test
        void segundoMonitorNaoConsegueAssumir() {
            assertThatThrownBy(() -> solicitacao.assumir(bruno, depois(6)))
                    .isInstanceOf(TransicaoInvalidaException.class)
                    .hasMessageContaining("já foi assumida por Ana Monitora");
        }

        @Test
        void responsavelConclui() {
            solicitacao.concluir(ana, "Explicado com exemplo de chat.", depois(25));

            assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
            assertThat(solicitacao.getObservacaoConclusao()).isEqualTo("Explicado com exemplo de chat.");
            assertThat(solicitacao.tempoDeAtendimento()).contains(Duration.ofMinutes(20));
        }

        @Test
        void outroMonitorNaoConclui() {
            assertThatThrownBy(() -> solicitacao.concluir(bruno, null, depois(10)))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void adminPodeConcluirPorOutroMonitor() {
            solicitacao.concluir(admin, null, depois(10));
            assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
        }

        @Test
        void devolverVoltaParaAFilaSemMonitor() {
            solicitacao.devolver(ana, depois(8));

            assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.ABERTA);
            assertThat(solicitacao.getMonitor()).isNull();
            assertThat(solicitacao.getAssumidaEm()).isNull();
            assertThat(solicitacao.getEventos()).extracting(EventoSolicitacao::getTipo)
                    .containsExactly(TipoEvento.CRIADA, TipoEvento.ASSUMIDA, TipoEvento.DEVOLVIDA);
        }

        @Test
        void depoisDeDevolvidaOutroMonitorPodeAssumir() {
            solicitacao.devolver(ana, depois(8));
            solicitacao.assumir(bruno, depois(9));
            assertThat(solicitacao.getMonitor()).isEqualTo(bruno);
        }

        @Test
        void alunoNaoCancelaDuranteOAtendimento() {
            assertThatThrownBy(() -> solicitacao.cancelar(aluno, depois(6)))
                    .isInstanceOf(TransicaoInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("link da reunião")
    class LinkDaReuniao {

        private static final String MEET = "https://meet.google.com/xyz-abcd-efg";

        @Test
        void alunoTrocaOLinkEnquantoEsperaNaFila() {
            solicitacao.definirLink(aluno, MEET, depois(1));
            assertThat(solicitacao.getLinkReuniao()).isEqualTo(MEET);
            assertThat(solicitacao.getEventos()).extracting(EventoSolicitacao::getTipo).endsWith(TipoEvento.LINK_DEFINIDO);
        }

        @Test
        void monitorNaoMexeNoLinkAntesDeAssumir() {
            assertThatThrownBy(() -> solicitacao.definirLink(ana, MEET, depois(1)))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void monitorResponsavelAdicionaOLinkDuranteOAtendimento() {
            Solicitacao semLink = Solicitacao.abrir(aluno, observer, "t", "d", Modalidade.ONLINE, null, INICIO);
            semLink.assumir(ana, depois(2));
            semLink.definirLink(ana, MEET, depois(3));
            assertThat(semLink.getLinkReuniao()).isEqualTo(MEET);
        }

        @Test
        void outroMonitorNaoTrocaOLink() {
            solicitacao.assumir(ana, depois(2));
            assertThatThrownBy(() -> solicitacao.definirLink(bruno, MEET, depois(3)))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void depoisDeConcluidaOLinkFicaCongelado() {
            solicitacao.assumir(ana, depois(2));
            solicitacao.concluir(ana, null, depois(3));
            assertThatThrownBy(() -> solicitacao.definirLink(ana, MEET, depois(4)))
                    .isInstanceOf(TransicaoInvalidaException.class)
                    .hasMessage("Não é possível trocar o link de uma solicitação concluída.");
        }

        @Test
        void atendimentoPresencialNaoTemLink() {
            Solicitacao presencial = Solicitacao.abrir(aluno, observer, "t", "d", Modalidade.PRESENCIAL, null, INICIO);
            assertThatThrownBy(() -> presencial.definirLink(aluno, MEET, depois(1)))
                    .isInstanceOf(br.monitoriamap.common.RegraNegocioException.class);
        }
    }

    @Nested
    @DisplayName("estados finais")
    class EstadosFinais {

        @Test
        void concluidaNaoAceitaNenhumaAcao() {
            solicitacao.assumir(ana, depois(1));
            solicitacao.concluir(ana, null, depois(2));

            assertThatThrownBy(() -> solicitacao.assumir(bruno, depois(3))).isInstanceOf(TransicaoInvalidaException.class);
            assertThatThrownBy(() -> solicitacao.devolver(ana, depois(3))).isInstanceOf(TransicaoInvalidaException.class);
            assertThatThrownBy(() -> solicitacao.cancelar(aluno, depois(3))).isInstanceOf(TransicaoInvalidaException.class);
            assertThat(StatusSolicitacao.CONCLUIDA.isFinal()).isTrue();
        }

        @Test
        void canceladaNaoPodeSerAssumida() {
            solicitacao.cancelar(aluno, depois(1));
            assertThatThrownBy(() -> solicitacao.assumir(ana, depois(2)))
                    .isInstanceOf(TransicaoInvalidaException.class)
                    .hasMessage("Não é possível assumir uma solicitação cancelada.");
        }
    }
}
