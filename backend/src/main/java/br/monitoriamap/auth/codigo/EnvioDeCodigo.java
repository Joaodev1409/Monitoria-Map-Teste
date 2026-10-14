package br.monitoriamap.auth.codigo;

import br.monitoriamap.auth.codigo.CodigoVerificacao.Finalidade;
import br.monitoriamap.usuario.Usuario;

/**
 * Como o código chega até a pessoa. Hoje é por e-mail ({@link EmailDeCodigo}); nos testes
 * trocamos por um dublê que só guarda o código, sem precisar de servidor de e-mail.
 */
public interface EnvioDeCodigo {

    void enviar(Usuario usuario, Finalidade finalidade, String codigo);
}
