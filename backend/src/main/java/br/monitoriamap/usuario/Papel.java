package br.monitoriamap.usuario;

/**
 * Papel do usuário no sistema. Cada papel inclui as permissões do anterior:
 * um MONITOR também pode abrir solicitações, e um ADMIN também atende.
 */
public enum Papel {
    ALUNO,
    MONITOR,
    ADMIN;

    public boolean podeAtender() {
        return this == MONITOR || this == ADMIN;
    }
}
