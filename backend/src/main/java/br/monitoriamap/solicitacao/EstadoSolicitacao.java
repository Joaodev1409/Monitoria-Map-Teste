package br.monitoriamap.solicitacao;

import br.monitoriamap.usuario.Usuario;

import java.time.Instant;

/**
 * Padrão State: cada estado da solicitação é uma classe que decide como reagir a cada ação.
 *
 * <p>Por padrão toda ação é inválida. Cada estado sobrescreve só as transições que permite,
 * então a {@link Solicitacao} não precisa de nenhum {@code if (status == ...)}.
 *
 * <pre>
 *   ABERTA ──assumir──▶ EM_ATENDIMENTO ──concluir──▶ CONCLUIDA
 *     │  ◀──devolver──────┘
 *     └──cancelar──▶ CANCELADA
 * </pre>
 *
 * <p>Os estados ficam no mesmo pacote da {@link Solicitacao} para poderem chamar os métodos
 * {@code registrar*}, que são package-private: fora deste pacote, a única forma de mudar
 * o status é pelas ações públicas, que passam sempre pelo estado atual.
 */
sealed interface EstadoSolicitacao
        permits EstadoAberta, EstadoEmAtendimento, EstadoConcluida, EstadoCancelada {

    StatusSolicitacao status();

    default void assumir(Solicitacao solicitacao, Usuario monitor, Instant agora) {
        throw TransicaoInvalidaException.de(status(), "assumir");
    }

    default void concluir(Solicitacao solicitacao, Usuario usuario, String observacao, Instant agora) {
        throw TransicaoInvalidaException.de(status(), "concluir");
    }

    default void devolver(Solicitacao solicitacao, Usuario usuario, Instant agora) {
        throw TransicaoInvalidaException.de(status(), "devolver à fila");
    }

    default void cancelar(Solicitacao solicitacao, Usuario usuario, Instant agora) {
        throw TransicaoInvalidaException.de(status(), "cancelar");
    }

    /** Não muda o estado, mas quem pode fazer depende dele: por isso também mora aqui. */
    default void definirLink(Solicitacao solicitacao, Usuario usuario, String link, Instant agora) {
        throw TransicaoInvalidaException.de(status(), "trocar o link de");
    }

    /**
     * A conversa só existe enquanto há atendimento: antes de alguém assumir não há com quem
     * falar, e depois de concluir ela fica só para leitura.
     */
    default MensagemSolicitacao enviarMensagem(Solicitacao solicitacao, Usuario autor, String texto, Instant agora) {
        throw new TransicaoInvalidaException(switch (status()) {
            case ABERTA -> "A conversa começa quando um monitor assumir o pedido.";
            case CONCLUIDA, CANCELADA -> "A conversa foi encerrada com o atendimento.";
            case EM_ATENDIMENTO -> "Não é possível enviar mensagens agora.";
        });
    }
}
