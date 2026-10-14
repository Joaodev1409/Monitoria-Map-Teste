package br.monitoriamap.solicitacao.dto;

import br.monitoriamap.solicitacao.Modalidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NovaSolicitacaoRequest(
        @NotNull(message = "Escolha um tópico.")
        Long topicoId,

        @NotBlank(message = "Dê um título para a sua dúvida.")
        @Size(max = 120, message = "O título pode ter até 120 caracteres.")
        String titulo,

        @NotBlank(message = "Descreva a sua dúvida.")
        @Size(max = 4000, message = "A descrição pode ter até 4000 caracteres.")
        String descricao,

        @NotNull(message = "Escolha a modalidade.")
        Modalidade modalidade,

        @Size(max = 500, message = "O link pode ter até 500 caracteres.")
        @Pattern(regexp = "^$|^https?://.+", message = "O link deve começar com http:// ou https://.")
        String linkReuniao
) {
}
