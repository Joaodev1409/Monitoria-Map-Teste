package br.monitoriamap.auth.codigo;

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

/** Código de 6 dígitos enviado por e-mail. Guardamos só o hash, como numa senha. */
@Entity
@Table(name = "codigo_verificacao")
public class CodigoVerificacao {

    static final int MAXIMO_TENTATIVAS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Finalidade finalidade;

    @Column(nullable = false)
    private String codigoHash;

    @Column(nullable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(nullable = false)
    private boolean usado;

    protected CodigoVerificacao() {
        // exigido pelo JPA
    }

    CodigoVerificacao(Usuario usuario, Finalidade finalidade, String codigoHash, Instant criadoEm, Instant expiraEm) {
        this.usuario = usuario;
        this.finalidade = finalidade;
        this.codigoHash = codigoHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    boolean podeSerUsado(Instant agora) {
        return !usado && tentativas < MAXIMO_TENTATIVAS && agora.isBefore(expiraEm);
    }

    void registrarTentativaErrada() {
        tentativas++;
    }

    void marcarUsado() {
        usado = true;
    }

    void invalidar() {
        usado = true;
    }

    int tentativasRestantes() {
        return Math.max(0, MAXIMO_TENTATIVAS - tentativas);
    }

    String getCodigoHash() {
        return codigoHash;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }

    public enum Finalidade {
        CADASTRO,
        REDEFINIR_SENHA
    }
}
