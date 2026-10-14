package br.monitoriamap.solicitacao.dto;

import jakarta.validation.constraints.Size;

public record ConcluirRequest(
        @Size(max = 2000, message = "A observação pode ter até 2000 caracteres.")
        String observacao
) {
}
