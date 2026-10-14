package br.monitoriamap.auth.codigo;

import br.monitoriamap.auth.codigo.CodigoVerificacao.Finalidade;
import br.monitoriamap.common.RegraNegocioException;
import br.monitoriamap.config.AppProperties;
import br.monitoriamap.usuario.Usuario;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Component
public class EmailDeCodigo implements EnvioDeCodigo {

    private static final Logger log = LoggerFactory.getLogger(EmailDeCodigo.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templates;
    private final AppProperties.Email config;
    private final Environment ambiente;

    public EmailDeCodigo(JavaMailSender mailSender, TemplateEngine templates, AppProperties propriedades,
                         Environment ambiente) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.config = propriedades.email();
        this.ambiente = ambiente;
    }

    @Override
    public void enviar(Usuario usuario, Finalidade finalidade, String codigo) {
        String assunto = finalidade == Finalidade.CADASTRO
                ? codigo + " é o seu código de confirmação"
                : codigo + " é o seu código para trocar a senha";

        if (!config.habilitado()) {
            // Só no perfil dev o código aparece no log, para dar para testar sem servidor de e-mail.
            if (ambiente.acceptsProfiles(Profiles.of("dev"))) {
                log.info("[e-mail desativado] Código para {}: {}", usuario.getEmail(), codigo);
            } else {
                log.warn("E-mail desativado: o código para {} não foi enviado (APP_EMAIL_HABILITADO).", usuario.getEmail());
            }
            return;
        }

        Context contexto = new Context();
        contexto.setVariable("nome", usuario.getNome());
        contexto.setVariable("codigo", codigo);
        contexto.setVariable("cadastro", finalidade == Finalidade.CADASTRO);
        try {
            MimeMessage email = mailSender.createMimeMessage();
            MimeMessageHelper ajudante = new MimeMessageHelper(email, false, StandardCharsets.UTF_8.name());
            ajudante.setFrom(config.remetente());
            ajudante.setTo(usuario.getEmail());
            ajudante.setSubject("[Monitoria MAP] " + assunto);
            ajudante.setText(templates.process("email/codigo", contexto), true);
            mailSender.send(email);
        } catch (MessagingException | MailException e) {
            log.error("Falha ao enviar código para {}", usuario.getEmail(), e);
            throw new RegraNegocioException("Não conseguimos enviar o e-mail agora. Tente de novo em instantes.");
        }
    }
}
