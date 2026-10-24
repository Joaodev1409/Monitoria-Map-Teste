package br.monitoriamap.notificacao;

import br.monitoriamap.config.AppProperties;
import br.monitoriamap.solicitacao.Solicitacao;
import br.monitoriamap.solicitacao.SolicitacaoAlteradaEvent;
import br.monitoriamap.solicitacao.TipoEvento;
import br.monitoriamap.solicitacao.dto.SolicitacaoDTO;
import br.monitoriamap.topico.Topico;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioRepository;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static br.monitoriamap.Fixtures.INICIO;
import static br.monitoriamap.Fixtures.solicitacao;
import static br.monitoriamap.Fixtures.topico;
import static br.monitoriamap.Fixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Quem recebe e-mail em cada evento (sem servidor de e-mail: o envio é um dublê). */
class EmailNotificadorTest {

    private final Usuario aluno = usuario(1, "Maria Aluna", Papel.ALUNO);
    private final Usuario ana = usuario(2, "Ana Monitora", Papel.MONITOR);
    private final Usuario bruno = usuario(3, "Bruno Admin", Papel.ADMIN);
    private final Topico observer = topico(1, "Observer");

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final TemplateEngine templates = mock(TemplateEngine.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);

    private Solicitacao solicitacao;

    @BeforeEach
    void preparar() {
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        when(templates.process(anyString(), any(IContext.class))).thenReturn("<p>corpo</p>");
        when(usuarios.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(usuarios.findByPapelInAndAtivoTrue(any())).thenReturn(List.of(ana, bruno));
        solicitacao = solicitacao(aluno, observer, INICIO);
    }

    private EmailNotificador notificador(boolean habilitado) {
        AppProperties propriedades = new AppProperties(null, null, null,
                new AppProperties.Email(habilitado, "Monitoria MAP <nao-responda@exemplo.com>"), null,
                "https://monitoria.exemplo", ZoneId.of("America/Fortaleza"));
        return new EmailNotificador(mailSender, templates, usuarios, propriedades);
    }

    private static SolicitacaoAlteradaEvent evento(TipoEvento tipo, Solicitacao s) {
        return new SolicitacaoAlteradaEvent(tipo, SolicitacaoDTO.detalhe(s));
    }

    private MimeMessage enviado() throws Exception {
        ArgumentCaptor<MimeMessage> email = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(email.capture());
        return email.getValue();
    }

    private static List<String> enderecos(Address[] enderecos) {
        return enderecos == null ? List.of() : Arrays.stream(enderecos).map(Address::toString).toList();
    }

    private static Instant depois(int minutos) {
        return INICIO.plus(Duration.ofMinutes(minutos));
    }

    @Test
    void pedidoNovoVaiParaOsMonitoresEmCopiaOculta() throws Exception {
        notificador(true).notificar(evento(TipoEvento.CRIADA, solicitacao));

        MimeMessage email = enviado();
        assertThat(email.getSubject()).isEqualTo("[Monitoria MAP] Nova solicitação: Dúvida no Observer");
        assertThat(enderecos(email.getRecipients(RecipientType.BCC)))
                .containsExactlyInAnyOrder(ana.getEmail(), bruno.getEmail());
        assertThat(enderecos(email.getRecipients(RecipientType.TO))).doesNotContain(ana.getEmail(), aluno.getEmail());
    }

    @Test
    void quemAbriuOPedidoNaoRecebeOAvisoDosMonitores() throws Exception {
        when(usuarios.findByPapelInAndAtivoTrue(any())).thenReturn(List.of(ana, aluno));

        notificador(true).notificar(evento(TipoEvento.CRIADA, solicitacao));

        assertThat(enderecos(enviado().getAllRecipients())).doesNotContain(aluno.getEmail());
    }

    @Test
    void pedidoAssumidoAvisaOAluno() throws Exception {
        solicitacao.assumir(ana, depois(1));

        notificador(true).notificar(evento(TipoEvento.ASSUMIDA, solicitacao));

        MimeMessage email = enviado();
        assertThat(enderecos(email.getRecipients(RecipientType.TO))).containsExactly(aluno.getEmail());
        assertThat(email.getSubject()).contains("Ana Monitora vai te ajudar");
    }

    @Test
    void linkEnviadoPeloMonitorAvisaOAluno() throws Exception {
        solicitacao.assumir(ana, depois(1));
        solicitacao.definirLink(ana, "https://meet.google.com/novo", depois(2));

        notificador(true).notificar(evento(TipoEvento.LINK_DEFINIDO, solicitacao));

        assertThat(enderecos(enviado().getRecipients(RecipientType.TO))).containsExactly(aluno.getEmail());
    }

    @Test
    void linkPostoPeloProprioAlunoNaoGeraEmail() {
        solicitacao.definirLink(aluno, "https://meet.google.com/meu", depois(1));

        notificador(true).notificar(evento(TipoEvento.LINK_DEFINIDO, solicitacao));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void cancelamentoNaoGeraEmail() {
        solicitacao.cancelar(aluno, depois(1));

        notificador(true).notificar(evento(TipoEvento.CANCELADA, solicitacao));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void comOEmailDesligadoNadaEEnviado() {
        notificador(false).notificar(evento(TipoEvento.CRIADA, solicitacao));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
