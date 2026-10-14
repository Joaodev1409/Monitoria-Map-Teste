package br.monitoriamap.usuario;

/**
 * O mínimo que outra pessoa precisa saber sobre alguém: nome e foto.
 * Sem e-mail de propósito: alunos e monitores não precisam do contato uns dos outros
 * para usar a fila (privacidade por padrão).
 */
public record UsuarioResumoDTO(Long id, String nome, String fotoUrl) {

    public static UsuarioResumoDTO de(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResumoDTO(usuario.getId(), usuario.getNome(), usuario.getFotoUrl());
    }
}
