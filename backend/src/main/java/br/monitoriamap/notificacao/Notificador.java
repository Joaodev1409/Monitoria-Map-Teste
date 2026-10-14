package br.monitoriamap.notificacao;

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

    default String nome() {
        return getClass().getSimpleName();
    }
}
