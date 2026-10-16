package br.monitoriamap.solicitacao;

import br.monitoriamap.solicitacao.dto.MensagemDTO;

/**
 * Publicado quando alguém escreve na conversa de um pedido. Leva os ids dos dois
 * participantes para quem notifica não precisar consultar o banco.
 */
public record MensagemEnviadaEvent(MensagemDTO mensagem, Long alunoId, Long monitorId) {
}
