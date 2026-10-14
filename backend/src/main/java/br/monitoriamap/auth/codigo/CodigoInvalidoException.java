package br.monitoriamap.auth.codigo;

import br.monitoriamap.common.RegraNegocioException;

/** Código errado, vencido ou já usado (HTTP 422). */
public class CodigoInvalidoException extends RegraNegocioException {

    public CodigoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
