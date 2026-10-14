package br.monitoriamap.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    private String fotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @Column(nullable = false)
    private boolean ativo = true;

    /** Reservado para a futura notificação por WhatsApp. */
    private String telefone;

    @Column(nullable = false)
    private Instant criadoEm;

    /** Tokens emitidos antes deste instante não valem mais (atualizado ao sair da conta). */
    private Instant sessoesValidasDesde;

    protected Usuario() {
        // exigido pelo JPA
    }

    public Usuario(String nome, String email, Papel papel, Instant criadoEm) {
        this.nome = Objects.requireNonNull(nome);
        this.email = Objects.requireNonNull(email).toLowerCase();
        this.papel = Objects.requireNonNull(papel);
        this.criadoEm = Objects.requireNonNull(criadoEm);
    }

    public void atualizarPerfil(String nome, String fotoUrl) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome;
        }
        this.fotoUrl = fotoUrl;
    }

    public void alterarPapel(Papel novoPapel) {
        this.papel = Objects.requireNonNull(novoPapel);
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public void definirTelefone(String telefone) {
        this.telefone = telefone;
    }

    /** Invalida todas as sessões abertas até agora, em qualquer aparelho. */
    public void encerrarSessoes(Instant agora) {
        this.sessoesValidasDesde = agora.truncatedTo(ChronoUnit.MILLIS);
    }

    /** Um token só vale se foi emitido depois do último "Sair". */
    public boolean sessaoAindaValida(Instant emitidaEm) {
        return sessoesValidasDesde == null || (emitidaEm != null && !emitidaEm.isBefore(sessoesValidasDesde));
    }

    public boolean podeAtender() {
        return papel.podeAtender();
    }

    public boolean isAdmin() {
        return papel == Papel.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public Papel getPapel() {
        return papel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public String getTelefone() {
        return telefone;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario outro)) return false;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
