package br.monitoriamap.auth;

import br.monitoriamap.auth.GoogleTokenVerifier.IdentidadeGoogle;
import br.monitoriamap.config.AppProperties;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioDTO;
import br.monitoriamap.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

/**
 * Login só com a conta Google da UEPB.
 *
 * <p>Os e-mails @aluno.uepb.edu.br e @servidor.uepb.edu.br são contas do Google Workspace da
 * universidade. Entrando por elas, o Google garante que a pessoa é mesmo dona do e-mail:
 * não precisamos guardar senha, mandar código nem ter "esqueci a senha".
 *
 * <p>Conferimos duas coisas: o domínio do e-mail e o claim {@code hd} do token, que o Google só
 * preenche para contas gerenciadas pela organização (uma conta pessoal não tem {@code hd}).
 */
@Service
@Transactional
public class AuthService {

    private final GoogleTokenVerifier google;
    private final TokenService tokens;
    private final UsuarioRepository usuarios;
    private final AppProperties.Auth config;
    private final Clock relogio;

    public AuthService(GoogleTokenVerifier google, TokenService tokens, UsuarioRepository usuarios,
                       AppProperties propriedades, Clock relogio) {
        this.google = google;
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.config = propriedades.auth();
        this.relogio = relogio;
    }

    public Login entrarComGoogle(String idToken) {
        IdentidadeGoogle identidade = google.verificar(idToken);
        if (!identidade.emailVerificado()) {
            throw new LoginInvalidoException("Seu e-mail do Google ainda não foi verificado.");
        }
        String email = identidade.email().trim().toLowerCase(Locale.ROOT);
        if (!contaDaInstituicao(email, identidade.dominioGoogle())) {
            throw new LoginInvalidoException(
                    "Entre com a sua conta da UEPB (@" + String.join(" ou @", config.dominiosPermitidos()) + ").");
        }

        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseGet(() -> new Usuario(nomeOuEmail(identidade), email, papelInicial(email), relogio.instant()));
        usuario.atualizarPerfil(identidade.nome(), identidade.fotoUrl());
        if (!usuario.isAtivo()) {
            throw new LoginInvalidoException("Sua conta foi desativada. Fale com a coordenação da monitoria.");
        }
        if (ehAdminConfigurado(email) && !usuario.isAdmin()) {
            usuario.alterarPapel(Papel.ADMIN);
        }
        usuarios.save(usuario);
        return new Login(UsuarioDTO.de(usuario), tokens.emitir(usuario));
    }

    /** Sair encerra a sessão de verdade: o token antigo para de valer mesmo que alguém o tenha copiado. */
    public void sair(Long usuarioId) {
        usuarios.findById(usuarioId).ifPresent(u -> u.encerrarSessoes(relogio.instant()));
    }

    public List<String> dominiosPermitidos() {
        return config.dominiosPermitidos();
    }

    /** Domínio do e-mail permitido E conta gerenciada pela organização (claim "hd" do mesmo domínio). */
    boolean contaDaInstituicao(String email, String dominioGoogle) {
        List<String> dominios = config.dominiosPermitidos().stream().map(d -> d.trim().toLowerCase(Locale.ROOT)).toList();
        if (dominios.isEmpty()) {
            return true;
        }
        String dominioEmail = email.substring(email.indexOf('@') + 1);
        return dominios.contains(dominioEmail)
                && dominioGoogle != null
                && dominioGoogle.equalsIgnoreCase(dominioEmail);
    }

    private Papel papelInicial(String email) {
        return ehAdminConfigurado(email) ? Papel.ADMIN : Papel.ALUNO;
    }

    private boolean ehAdminConfigurado(String email) {
        return config.adminEmails().stream().anyMatch(admin -> admin.trim().equalsIgnoreCase(email));
    }

    private static String nomeOuEmail(IdentidadeGoogle identidade) {
        return identidade.nome() != null && !identidade.nome().isBlank() ? identidade.nome() : identidade.email();
    }

    /** Resultado interno do login: os dados para a resposta e o token para o cookie. */
    public record Login(UsuarioDTO usuario, TokenService.TokenEmitido token) {
    }
}
