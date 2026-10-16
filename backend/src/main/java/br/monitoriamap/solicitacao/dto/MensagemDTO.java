package br.monitoriamap.solicitacao.dto;

import br.monitoriamap.solicitacao.MensagemSolicitacao;
import br.monitoriamap.usuario.UsuarioResumoDTO;

import java.time.Instant;

/** Uma mensagem da conversa. O autor vai resumido (nome e foto), sem e-mail. */
public record MensagemDTO(Long id, Long solicitacaoId, UsuarioResumoDTO autor, String texto, Instant enviadaEm) {

    public static MensagemDTO de(MensagemSolicitacao mensagem) {
        return new MensagemDTO(mensagem.getId(), mensagem.getSolicitacao().getId(),
                UsuarioResumoDTO.de(mensagem.getAutor()), mensagem.getTexto(), mensagem.getEnviadaEm());
    }
}
