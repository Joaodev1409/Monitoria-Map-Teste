package br.monitoriamap.notificacao;

import br.monitoriamap.config.AppProperties;
import br.monitoriamap.solicitacao.SolicitacaoAlteradaEvent;
import br.monitoriamap.solicitacao.dto.SolicitacaoDTO;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Envia e-mails nos momentos que importam:
 * <ul>
 *   <li>nova solicitação ou solicitação devolvida → todos os monitores;</li>
 *   <li>solicitação assumida ou concluída → o aluno.</li>
 * </ul>
 * Com {@code app.email.habilitado=false} (padrão no desenvolvimento) apenas registra no log.
 */
@Component
public class EmailNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificador.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templates;
    private final UsuarioRepository usuarios;
    private final AppProperties propriedades;

    public EmailNotificador(JavaMailSender mailSender, TemplateEngine templates, UsuarioRepository usuarios,
                            AppProperties propriedades) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.usuarios = usuarios;
        this.propriedades = propriedades;
    }

    @Override
    public void notificar(SolicitacaoAlteradaEvent evento) {
        SolicitacaoDTO s = evento.solicitacao();
        switch (evento.tipo()) {
            case CRIADA -> enviarParaMonitores(s,
                    "Nova solicitação: " + s.titulo(),
                    s.aluno().nome() + " pediu ajuda em " + s.topico().nome() + ".",
                    "Ver fila", "/monitor/fila");
            case DEVOLVIDA -> enviarParaMonitores(s,
                    "De volta à fila: " + s.titulo(),
                    "A solicitação de " + s.aluno().nome() + " voltou para a fila e precisa de um monitor.",
                    "Ver fila", "/monitor/fila");
            case ASSUMIDA -> enviar(emailDoAluno(s), s,
                    s.monitor().nome() + " vai te ajudar",
                    s.monitor().nome() + " assumiu a sua solicitação e vai entrar em contato.",
                    "Acompanhar", "/solicitacoes/" + s.id());
            case CONCLUIDA -> enviar(emailDoAluno(s), s,
                    "Atendimento concluído: " + s.titulo(),
                    "O atendimento foi marcado como concluído. Se a dúvida continuar, abra uma nova solicitação.",
                    "Ver detalhes", "/solicitacoes/" + s.id());
            case LINK_DEFINIDO -> {
                // Só avisa por e-mail quando foi o monitor que mandou o link (o aluno já sabe o que colocou).
                var ultimo = s.historico().isEmpty() ? null : s.historico().get(s.historico().size() - 1);
                if (ultimo != null && !ultimo.autor().id().equals(s.aluno().id())) {
                    enviar(emailDoAluno(s), s,
                            "Link da reunião: " + s.titulo(),
                            ultimo.autor().nome() + " enviou o link da chamada. Abra o pedido para entrar.",
                            "Ver o link", "/solicitacoes/" + s.id());
                }
            }
            case CANCELADA -> {
                // Cancelamento é feito pelo próprio aluno: ninguém precisa ser avisado por e-mail.
            }
        }
    }

    /** O e-mail não viaja no evento (que também vai para o navegador): buscamos no banco. */
    private List<String> emailDoAluno(SolicitacaoDTO s) {
        return usuarios.findById(s.aluno().id()).map(Usuario::getEmail).map(List::of).orElse(List.of());
    }

    private void enviarParaMonitores(SolicitacaoDTO s, String assunto, String mensagem, String botao, String caminho) {
        List<String> emails = usuarios.findByPapelInAndAtivoTrue(List.of(Papel.MONITOR, Papel.ADMIN)).stream()
                .filter(u -> !u.getId().equals(s.aluno().id()))
                .map(Usuario::getEmail)
                .toList();
        enviar(emails, s, assunto, mensagem, botao, caminho);
    }

    private void enviar(List<String> destinatarios, SolicitacaoDTO s, String assunto, String mensagem,
                        String botao, String caminho) {
        if (destinatarios.isEmpty()) {
            return;
        }
        if (!propriedades.email().habilitado()) {
            log.info("[e-mail desativado] Para {}: {}", destinatarios, assunto);
            return;
        }

        Context contexto = new Context();
        contexto.setVariable("titulo", assunto);
        contexto.setVariable("mensagem", mensagem);
        contexto.setVariable("solicitacao", s);
        contexto.setVariable("botao", botao);
        contexto.setVariable("link", propriedades.urlFrontend() + caminho);
        String html = templates.process("email/notificacao", contexto);

        try {
            MimeMessage email = mailSender.createMimeMessage();
            MimeMessageHelper ajudante = new MimeMessageHelper(email, false, StandardCharsets.UTF_8.name());
            ajudante.setFrom(propriedades.email().remetente());
            // Monitores vão em cópia oculta: ninguém vê o e-mail dos outros.
            if (destinatarios.size() == 1) {
                ajudante.setTo(destinatarios.get(0));
            } else {
                ajudante.setTo(propriedades.email().remetente());
                ajudante.setBcc(destinatarios.toArray(String[]::new));
            }
            ajudante.setSubject("[Monitoria MAP] " + assunto);
            ajudante.setText(html, true);
            mailSender.send(email);
        } catch (MessagingException e) {
            throw new IllegalStateException("Não foi possível montar o e-mail", e);
        }
    }
}
