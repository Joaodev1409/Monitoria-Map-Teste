package br.monitoriamap.topico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/** Assunto da dúvida (ex.: "Padrões comportamentais", "Lista 3"). Cadastrado pelo admin. */
@Entity
@Table(name = "topico")
public class Topico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Topico() {
        // exigido pelo JPA
    }

    public Topico(String nome) {
        this.nome = Objects.requireNonNull(nome).trim();
    }

    public void renomear(String nome) {
        this.nome = Objects.requireNonNull(nome).trim();
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
