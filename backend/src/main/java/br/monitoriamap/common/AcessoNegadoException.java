package br.monitoriamap.common;

/** O usuário está autenticado, mas não pode executar esta ação sobre este recurso. */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
