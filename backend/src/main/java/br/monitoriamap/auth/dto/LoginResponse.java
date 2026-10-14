package br.monitoriamap.auth.dto;

import br.monitoriamap.usuario.UsuarioDTO;

import java.time.Instant;

/** Resposta do login. O token NÃO vem aqui: ele vai no cookie HttpOnly da resposta. */
public record LoginResponse(UsuarioDTO usuario, Instant sessaoExpiraEm) {
}
