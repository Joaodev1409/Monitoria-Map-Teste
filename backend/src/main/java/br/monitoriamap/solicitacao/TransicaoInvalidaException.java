package br.monitoriamap.solicitacao;

/** A ação pedida não é permitida no estado atual da solicitação (vira HTTP 409). */
public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    static TransicaoInvalidaException de(StatusSolicitacao status, String acao) {
        return new TransicaoInvalidaException(
                "Não é possível " + acao + " uma solicitação " + status.descricao() + ".");
    }
}
