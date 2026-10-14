package br.monitoriamap.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code credential} é o ID token devolvido pelo Google Identity Services no front-end. */
public record LoginGoogleRequest(@NotBlank String credential) {
}
