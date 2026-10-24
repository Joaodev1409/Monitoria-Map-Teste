package br.monitoriamap.notificacao;

import br.monitoriamap.solicitacao.MensagemEnviadaEvent;
import br.monitoriamap.solicitacao.SolicitacaoAlteradaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.function.Consumer;

/**
 * Padrão Observer: o {@code SolicitacaoService} publica um evento sem saber quem está ouvindo.
 * Este componente é o observador que repassa o evento a todos os {@link Notificador}es.
 *
 * <ul>
 *   <li>{@code AFTER_COMMIT}: só notifica se a transação deu certo (nada de avisar sobre algo
 *       que foi desfeito).</li>
 *   <li>{@code @Async}: o envio de e-mail não atrasa a resposta para quem clicou no botão.</li>
 *   <li>Cada notificador roda isolado: se o e-mail falhar, o painel em tempo real ainda é avisado.</li>
 * </ul>
 */
@Component
public class DespachanteDeNotificacoes {

    private static final Logger log = LoggerFactory.getLogger(DespachanteDeNotificacoes.class);

    private final List<Notificador> notificadores;

    public DespachanteDeNotificacoes(List<Notificador> notificadores) {
        this.notificadores = notificadores;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoAlterarSolicitacao(SolicitacaoAlteradaEvent evento) {
        avisarTodos("a solicitação " + evento.solicitacao().id(), notificador -> notificador.notificar(evento));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoEnviarMensagem(MensagemEnviadaEvent evento) {
        avisarTodos("a mensagem da solicitação " + evento.mensagem().solicitacaoId(),
                notificador -> notificador.notificarMensagem(evento));
    }

    /** Chama cada canal isoladamente: se um falhar, os outros ainda são avisados. */
    private void avisarTodos(String sobre, Consumer<Notificador> aviso) {
        for (Notificador notificador : notificadores) {
            try {
                aviso.accept(notificador);
            } catch (RuntimeException e) {
                log.warn("Falha no notificador {} para {}: {}", notificador.nome(), sobre, e.getMessage(), e);
            }
        }
    }
}
