package br.monitoriamap.usuario;

import java.time.Instant;

public record UsuarioDTO(Long id, String nome, String email, String fotoUrl, Papel papel, boolean ativo,
                         String telefone, Instant criadoEm) {

    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNome(), u.getEmail(), u.getFotoUrl(), u.getPapel(), u.isAtivo(),
                u.getTelefone(), u.getCriadoEm());
    }
}
