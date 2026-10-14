package br.monitoriamap.auth;

/** A senha confere, mas a pessoa ainda não confirmou o e-mail: o front pede o código (HTTP 403). */
public class EmailNaoVerificadoException extends RuntimeException {

    public EmailNaoVerificadoException() {
        super("Confirme o seu e-mail para entrar. Enviamos um código para você.");
    }
}
