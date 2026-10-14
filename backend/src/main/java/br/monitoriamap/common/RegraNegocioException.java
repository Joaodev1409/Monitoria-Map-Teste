package br.monitoriamap.common;

/** Violação de uma regra de negócio que não depende do estado da solicitação. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
