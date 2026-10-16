package br.monitoriamap.notificacao;

import br.monitoriamap.solicitacao.MensagemEnviadaEvent;
import br.monitoriamap.solicitacao.SolicitacaoAlteradaEvent;

/**
 * Padrão Strategy: cada canal de notificação é uma implementação desta interface.
 *
 * <p>Para criar um canal novo (ex.: WhatsApp), basta escrever uma classe
 * {@code @Component} que implemente {@code Notificador}. O {@link DespachanteDeNotificacoes}
 * recebe todas as implementações por injeção e passa a chamá-la sem nenhuma alteração.
 * Esse é o princípio Aberto/Fechado (o "O" do SOLID).
 */
public interface Notificador {

    void notificar(SolicitacaoAlteradaEvent evento);

    /**
     * Mensagem nova na conversa de um pedido. Por padrão, o canal ignora: hoje só o painel em
     * tempo real entrega mensagens, e o e-mail não precisou mudar nada para isso.
     */
    default void notificarMensagem(MensagemEnviadaEvent evento) {
    }

    default String nome() {
        return getClass().getSimpleName();
    }
}
