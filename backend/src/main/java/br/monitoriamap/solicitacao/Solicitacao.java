package br.monitoriamap.solicitacao;

import br.monitoriamap.common.RegraNegocioException;
import br.monitoriamap.topico.Topico;
import br.monitoriamap.usuario.Usuario;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pedido de ajuda de um aluno. As ações públicas ({@link #assumir}, {@link #concluir},
 * {@link #devolver}, {@link #cancelar}) delegam para o estado atual (padrão State),
 * que valida a transição e só então chama o {@code registrar*} correspondente.
 */
@Entity
@Table(name = "solicitacao")
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id")
    private Usuario monitor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topico_id")
    private Topico topico;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalidade modalidade;

    private String linkReuniao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao status;

    @Column(nullable = false)
    private Instant criadaEm;

    private Instant assumidaEm;

    private Instant concluidaEm;

    private Instant canceladaEm;

    @Column(columnDefinition = "text")
    private String observacaoConclusao;

    /**
     * Lock otimista: se dois monitores assumirem ao mesmo tempo, o segundo commit
     * falha porque a versão mudou, e ele recebe um 409 em vez de "roubar" o atendimento.
     */
    @Version
    private Long versao;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ocorridoEm ASC, id ASC")
    private List<EventoSolicitacao> eventos = new ArrayList<>();

    protected Solicitacao() {
        // exigido pelo JPA
    }

    private Solicitacao(Usuario aluno, Topico topico, String titulo, String descricao,
                        Modalidade modalidade, String linkReuniao, Instant agora) {
        this.aluno = Objects.requireNonNull(aluno);
        this.topico = Objects.requireNonNull(topico);
        this.titulo = Objects.requireNonNull(titulo).trim();
        this.descricao = Objects.requireNonNull(descricao).trim();
        this.modalidade = Objects.requireNonNull(modalidade);
        this.linkReuniao = modalidade == Modalidade.ONLINE ? vazioParaNulo(linkReuniao) : null;
        this.status = StatusSolicitacao.ABERTA;
        this.criadaEm = Objects.requireNonNull(agora);
        registrarEvento(TipoEvento.CRIADA, aluno, agora, null);
    }

    public static Solicitacao abrir(Usuario aluno, Topico topico, String titulo, String descricao,
                                    Modalidade modalidade, String linkReuniao, Instant agora) {
        return new Solicitacao(aluno, topico, titulo, descricao, modalidade, linkReuniao, agora);
    }

    // ---- Ações públicas: todas passam pelo estado atual -------------------------------

    public void assumir(Usuario monitor, Instant agora) {
        status.estado().assumir(this, monitor, agora);
    }

    public void concluir(Usuario usuario, String observacao, Instant agora) {
        status.estado().concluir(this, usuario, observacao, agora);
    }

    public void devolver(Usuario usuario, Instant agora) {
        status.estado().devolver(this, usuario, agora);
    }

    public void cancelar(Usuario usuario, Instant agora) {
        status.estado().cancelar(this, usuario, agora);
    }

    /** Link do Meet/Zoom de um atendimento online, definido depois de abrir (pelo aluno ou pelo monitor). */
    public void definirLink(Usuario usuario, String link, Instant agora) {
        if (modalidade != Modalidade.ONLINE) {
            throw new RegraNegocioException("Só atendimentos online têm link de reunião.");
        }
        status.estado().definirLink(this, usuario, link, agora);
    }

    /**
     * Mensagem na conversa do pedido. Quem pode escrever depende do estado (só em atendimento,
     * só os dois envolvidos), então a decisão também fica com ele.
     */
    public MensagemSolicitacao enviarMensagem(Usuario autor, String texto, Instant agora) {
        if (texto == null || texto.isBlank()) {
            throw new RegraNegocioException("Escreva a mensagem antes de enviar.");
        }
        return status.estado().enviarMensagem(this, autor, texto.strip(), agora);
    }

    // ---- Mutações chamadas pelos estados (package-private de propósito) ---------------

    void registrarAssuncao(Usuario monitor, Instant agora) {
        this.monitor = monitor;
        this.assumidaEm = agora;
        this.status = StatusSolicitacao.EM_ATENDIMENTO;
        registrarEvento(TipoEvento.ASSUMIDA, monitor, agora, null);
    }

    void registrarConclusao(Usuario autor, String observacao, Instant agora) {
        this.concluidaEm = agora;
        this.observacaoConclusao = vazioParaNulo(observacao);
        this.status = StatusSolicitacao.CONCLUIDA;
        registrarEvento(TipoEvento.CONCLUIDA, autor, agora, this.observacaoConclusao);
    }

    void registrarDevolucao(Usuario autor, Instant agora) {
        String quemAtendia = monitor.getNome();
        this.monitor = null;
        this.assumidaEm = null;
        this.status = StatusSolicitacao.ABERTA;
        registrarEvento(TipoEvento.DEVOLVIDA, autor, agora, "Estava com " + quemAtendia);
    }

    void registrarCancelamento(Usuario autor, Instant agora) {
        this.canceladaEm = agora;
        this.status = StatusSolicitacao.CANCELADA;
        registrarEvento(TipoEvento.CANCELADA, autor, agora, null);
    }

    void registrarLink(Usuario autor, String link, Instant agora) {
        this.linkReuniao = vazioParaNulo(link);
        registrarEvento(TipoEvento.LINK_DEFINIDO, autor, agora, null);
    }

    MensagemSolicitacao criarMensagem(Usuario autor, String texto, Instant agora) {
        return new MensagemSolicitacao(this, autor, texto, agora);
    }

    private void registrarEvento(TipoEvento tipo, Usuario autor, Instant agora, String detalhe) {
        eventos.add(new EventoSolicitacao(this, tipo, autor, agora, detalhe));
    }

    // ---- Consultas ---------------------------------------------------------------------

    public boolean foiAbertaPor(Usuario usuario) {
        return aluno.getId() != null && aluno.getId().equals(usuario.getId());
    }

    public boolean estaSendoAtendidaPor(Usuario usuario) {
        return monitor != null && monitor.getId() != null && monitor.getId().equals(usuario.getId());
    }

    /**
     * Quem lê a conversa: o aluno, o monitor que está (ou esteve, se já concluiu) com o pedido
     * e o admin. Vale em qualquer estado: depois de concluir, o registro continua disponível.
     * Se o pedido voltar para a fila e outro monitor assumir, ele herda a conversa.
     */
    public boolean podeLerConversa(Usuario usuario) {
        return foiAbertaPor(usuario) || estaSendoAtendidaPor(usuario) || usuario.isAdmin();
    }

    /** Quanto tempo o aluno esperou até alguém assumir (vazio se ninguém assumiu). */
    public Optional<Duration> tempoDeEspera() {
        return Optional.ofNullable(assumidaEm).map(fim -> Duration.between(criadaEm, fim));
    }

    /** Duração do atendimento em si (vazio se ainda não foi concluída). */
    public Optional<Duration> tempoDeAtendimento() {
        if (assumidaEm == null || concluidaEm == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(assumidaEm, concluidaEm));
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    // ---- Getters -----------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public Usuario getAluno() {
        return aluno;
    }

    public Usuario getMonitor() {
        return monitor;
    }

    public Topico getTopico() {
        return topico;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public String getLinkReuniao() {
        return linkReuniao;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAssumidaEm() {
        return assumidaEm;
    }

    public Instant getConcluidaEm() {
        return concluidaEm;
    }

    public Instant getCanceladaEm() {
        return canceladaEm;
    }

    public String getObservacaoConclusao() {
        return observacaoConclusao;
    }

    public Long getVersao() {
        return versao;
    }

    public List<EventoSolicitacao> getEventos() {
        return Collections.unmodifiableList(eventos);
    }
}
