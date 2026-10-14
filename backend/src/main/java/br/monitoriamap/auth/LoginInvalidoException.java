package br.monitoriamap.auth;

/** Falha ao entrar: token do Google inválido, domínio não permitido, conta desativada... (HTTP 401). */
public class LoginInvalidoException extends RuntimeException {

    public LoginInvalidoException(String mensagem) {
        super(mensagem);
    }
}
