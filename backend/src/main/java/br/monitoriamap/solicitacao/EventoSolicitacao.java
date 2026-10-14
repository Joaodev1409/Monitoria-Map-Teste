package br.monitoriamap.solicitacao;

import br.monitoriamap.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Linha da trilha de auditoria: quem fez o quê, e quando. */
@Entity
@Table(name = "evento_solicitacao")
public class EventoSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvento tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(nullable = false)
    private Instant ocorridoEm;

    private String detalhe;

    protected EventoSolicitacao() {
        // exigido pelo JPA
    }

    EventoSolicitacao(Solicitacao solicitacao, TipoEvento tipo, Usuario autor, Instant ocorridoEm, String detalhe) {
        this.solicitacao = solicitacao;
        this.tipo = tipo;
        this.autor = autor;
        this.ocorridoEm = ocorridoEm;
        this.detalhe = detalhe;
    }

    public Long getId() {
        return id;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public Usuario getAutor() {
        return autor;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public String getDetalhe() {
        return detalhe;
    }
}
