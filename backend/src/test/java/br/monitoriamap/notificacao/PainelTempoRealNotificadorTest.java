package br.monitoriamap.notificacao;

import br.monitoriamap.auth.UsuarioAutenticado;
import br.monitoriamap.solicitacao.MensagemEnviadaEvent;
import br.monitoriamap.solicitacao.Solicitacao;
import br.monitoriamap.solicitacao.SolicitacaoAlteradaEvent;
import br.monitoriamap.solicitacao.TipoEvento;
import br.monitoriamap.solicitacao.dto.MensagemDTO;
import br.monitoriamap.solicitacao.dto.SolicitacaoDTO;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioResumoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.monitoriamap.Fixtures.INICIO;
import static br.monitoriamap.Fixtures.solicitacao;
import static br.monitoriamap.Fixtures.topico;
import static br.monitoriamap.Fixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;

/** Quem recebe o quê pelo canal ao vivo. */
class PainelTempoRealNotificadorTest {

    private final Usuario aluno = usuario(1, "Maria Aluna", Papel.ALUNO);
    private final Usuario outroAluno = usuario(2, "Outro Aluno", Papel.ALUNO);
    private final Usuario ana = usuario(3, "Ana Monitora", Papel.MONITOR);
    private final Usuario bruno = usuario(4, "Bruno Monitor", Papel.MONITOR);

    private final PainelTempoRealNotificador painel = new PainelTempoRealNotificador();

    /** Emissor de mentira: em vez de escrever na resposta HTTP, anota o nome de cada evento. */
    private static final class EmissorDeTeste extends SseEmitter {
        private static final Pattern NOME = Pattern.compile("event:(\\w+)");
        final List<String> recebidos = new ArrayList<>();

        @Override
        public void send(SseEventBuilder evento) {
            String texto = evento.build().stream().map(d -> String.valueOf(d.getData())).reduce("", String::concat);
            Matcher nome = NOME.matcher(texto);
            if (nome.find() && !nome.group(1).equals("conectado")) {
                recebidos.add(nome.group(1));
            }
        }
    }

    private EmissorDeTeste conectar(Usuario usuario) {
        EmissorDeTeste emissor = new EmissorDeTeste();
        painel.conectar(UsuarioAutenticado.de(usuario), emissor);
        return emissor;
    }

    @Test
    void mudancaNoPedidoVaiParaOsMonitoresEParaODono() {
        EmissorDeTeste doAluno = conectar(aluno);
        EmissorDeTeste doOutroAluno = conectar(outroAluno);
        EmissorDeTeste daAna = conectar(ana);
        Solicitacao s = solicitacao(aluno, topico(1, "Observer"), INICIO);

        painel.notificar(new SolicitacaoAlteradaEvent(TipoEvento.CRIADA, SolicitacaoDTO.detalhe(s)));

        assertThat(doAluno.recebidos).containsExactly("solicitacao");
        assertThat(daAna.recebidos).containsExactly("solicitacao");
        assertThat(doOutroAluno.recebidos).isEmpty();
    }

    @Test
    void mensagemDaConversaVaiSoParaOsDoisParticipantes() {
        EmissorDeTeste doAluno = conectar(aluno);
        EmissorDeTeste daAna = conectar(ana);
        EmissorDeTeste doBruno = conectar(bruno);
        MensagemDTO mensagem = new MensagemDTO(10L, 7L, UsuarioResumoDTO.de(aluno), "oi", INICIO);

        painel.notificarMensagem(new MensagemEnviadaEvent(mensagem, aluno.getId(), ana.getId()));

        assertThat(doAluno.recebidos).containsExactly("mensagem");
        assertThat(daAna.recebidos).containsExactly("mensagem");
        assertThat(doBruno.recebidos).as("outro monitor não vê a conversa").isEmpty();
    }
}
