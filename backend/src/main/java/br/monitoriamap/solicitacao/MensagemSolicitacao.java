package br.monitoriamap.solicitacao;

import br.monitoriamap.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Uma mensagem da conversa do pedido. Só nasce por {@link Solicitacao#enviarMensagem}, que
 * pergunta ao estado atual se a conversa está aberta e se quem escreve participa dela.
 *
 * <p>Não é uma coleção dentro da {@link Solicitacao}: a conversa pode crescer bastante e é
 * carregada à parte, só por quem pode lê-la ({@link MensagemRepository}).
 */
@Entity
@Table(name = "mensagem_solicitacao")
public class MensagemSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(nullable = false, columnDefinition = "text")
    private String texto;

    @Column(nullable = false)
    private Instant enviadaEm;

    protected MensagemSolicitacao() {
        // exigido pelo JPA
    }

    MensagemSolicitacao(Solicitacao solicitacao, Usuario autor, String texto, Instant enviadaEm) {
        this.solicitacao = solicitacao;
        this.autor = autor;
        this.texto = texto;
        this.enviadaEm = enviadaEm;
    }

    public Long getId() {
        return id;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public Usuario getAutor() {
        return autor;
    }

    public String getTexto() {
        return texto;
    }

    public Instant getEnviadaEm() {
        return enviadaEm;
    }
}
