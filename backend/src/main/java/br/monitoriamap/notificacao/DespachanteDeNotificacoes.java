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
        for (Notificador notificador : notificadores) {
            try {
                notificador.notificar(evento);
            } catch (RuntimeException e) {
                log.warn("Falha no notificador {} para a solicitação {}: {}",
                        notificador.nome(), evento.solicitacao().id(), e.getMessage(), e);
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoEnviarMensagem(MensagemEnviadaEvent evento) {
        for (Notificador notificador : notificadores) {
            try {
                notificador.notificarMensagem(evento);
            } catch (RuntimeException e) {
                log.warn("Falha no notificador {} para a mensagem da solicitação {}: {}",
                        notificador.nome(), evento.mensagem().solicitacaoId(), e.getMessage(), e);
            }
        }
    }
}
