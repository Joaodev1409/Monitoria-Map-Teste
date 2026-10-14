package br.monitoriamap.auth;

import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;

/** Quem está fazendo a requisição. Use com {@code @AuthenticationPrincipal} nos controllers. */
public record UsuarioAutenticado(Long id, String email, String nome, Papel papel) {

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getNome(), usuario.getPapel());
    }

    public boolean podeAtender() {
        return papel.podeAtender();
    }
}
