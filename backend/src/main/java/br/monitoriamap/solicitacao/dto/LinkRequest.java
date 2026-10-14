package br.monitoriamap.solicitacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LinkRequest(
        @NotBlank(message = "Cole o link da reunião.")
        @Size(max = 500, message = "O link pode ter até 500 caracteres.")
        @Pattern(regexp = "^https?://\\S+$", message = "O link deve começar com http:// ou https://.")
        String linkReuniao
) {
}
