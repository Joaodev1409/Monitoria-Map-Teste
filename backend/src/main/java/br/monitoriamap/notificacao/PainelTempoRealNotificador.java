package br.monitoriamap.notificacao;

import br.monitoriamap.auth.UsuarioAutenticado;
import br.monitoriamap.solicitacao.MensagemEnviadaEvent;
import br.monitoriamap.solicitacao.SolicitacaoAlteradaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Empurra as mudanças para quem está com o sistema aberto, via Server-Sent Events (SSE).
 *
 * <p>Monitores recebem tudo (para a fila atualizar sozinha); o aluno recebe apenas
 * o que acontece com as próprias solicitações. Mensagens da conversa vão só para os dois
 * participantes: os outros monitores não recebem.
 */
@Component
public class PainelTempoRealNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(PainelTempoRealNotificador.class);
    private static final long TEMPO_MAXIMO_CONEXAO = Duration.ofMinutes(30).toMillis();

    private final List<Conexao> conexoes = new CopyOnWriteArrayList<>();

    public SseEmitter conectar(UsuarioAutenticado usuario) {
        return conectar(usuario, new SseEmitter(TEMPO_MAXIMO_CONEXAO));
    }

    /** Recebe o emissor pronto: nos testes, um emissor que só anota o que recebeu. */
    SseEmitter conectar(UsuarioAutenticado usuario, SseEmitter emitter) {
        Conexao conexao = new Conexao(usuario.id(), usuario.podeAtender(), emitter);
        conexoes.add(conexao);

        emitter.onCompletion(() -> conexoes.remove(conexao));
        emitter.onTimeout(() -> conexoes.remove(conexao));
        emitter.onError(erro -> conexoes.remove(conexao));

        enviar(conexao, SseEmitter.event().name("conectado").data("ok"));
        return emitter;
    }

    @Override
    public void notificar(SolicitacaoAlteradaEvent evento) {
        Long alunoId = evento.solicitacao().aluno().id();
        Set<Long> interessados = evento.solicitacao().monitor() == null
                ? Set.of(alunoId)
                : Set.of(alunoId, evento.solicitacao().monitor().id());
        transmitir("solicitacao", evento,
                conexao -> conexao.podeAtender() || interessados.contains(conexao.usuarioId()));
    }

    @Override
    public void notificarMensagem(MensagemEnviadaEvent evento) {
        Set<Long> participantes = Set.of(evento.alunoId(), evento.monitorId());
        transmitir("mensagem", evento.mensagem(), conexao -> participantes.contains(conexao.usuarioId()));
    }

    /** Envia um evento com este nome e estes dados para cada conexão que passar no filtro. */
    private void transmitir(String nome, Object dados, Predicate<Conexao> destinatario) {
        for (Conexao conexao : conexoes) {
            if (destinatario.test(conexao)) {
                enviar(conexao, SseEmitter.event()
                        .name(nome)
                        .id(String.valueOf(System.currentTimeMillis()))
                        .data(dados));
            }
        }
    }

    /** Comentário vazio a cada 25s: evita que proxies (Render, nginx...) derrubem a conexão ociosa. */
    @Scheduled(fixedRate = 25_000)
    void manterConexoesVivas() {
        for (Conexao conexao : conexoes) {
            enviar(conexao, SseEmitter.event().comment("ping"));
        }
    }

    private void enviar(Conexao conexao, SseEmitter.SseEventBuilder evento) {
        try {
            conexao.emitter().send(evento);
        } catch (IOException | IllegalStateException e) {
            log.debug("Conexão SSE do usuário {} encerrada: {}", conexao.usuarioId(), e.getMessage());
            conexoes.remove(conexao);
        }
    }

    private record Conexao(Long usuarioId, boolean podeAtender, SseEmitter emitter) {
    }
}
