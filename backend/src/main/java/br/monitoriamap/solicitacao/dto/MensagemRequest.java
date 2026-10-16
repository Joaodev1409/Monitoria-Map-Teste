package br.monitoriamap.solicitacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensagemRequest(
        @NotBlank(message = "Escreva a mensagem antes de enviar.")
        @Size(max = 4000, message = "A mensagem pode ter até 4000 caracteres.")
        String texto
) {
}
