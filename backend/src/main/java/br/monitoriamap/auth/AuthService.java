package br.monitoriamap.auth;

import br.monitoriamap.auth.GoogleTokenVerifier.IdentidadeGoogle;
import br.monitoriamap.auth.codigo.CodigoInvalidoException;
import br.monitoriamap.auth.codigo.CodigoService;
import br.monitoriamap.auth.codigo.CodigoVerificacao.Finalidade;
import br.monitoriamap.auth.dto.Pedidos;
import br.monitoriamap.common.RegraNegocioException;
import br.monitoriamap.config.AppProperties;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioDTO;
import br.monitoriamap.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Contas da monitoria: cadastro com e-mail institucional + senha, confirmado por código.
 *
 * <pre>
 *  cadastrar ──▶ (código no e-mail) ──▶ confirmar ──▶ logado
 *  entrar ─────▶ senha confere? ──▶ e-mail confirmado? ──▶ logado
 *  esqueci a senha ──▶ (código no e-mail) ──▶ redefinir ──▶ logado
 * </pre>
 *
 * {@code noRollbackFor}: quando o código está errado, a tentativa precisa ficar gravada
 * mesmo com a exceção (senão daria para chutar códigos sem limite).
 */
@Service
@Transactional(noRollbackFor = CodigoInvalidoException.class)
public class AuthService {

    /** Hash de uma senha qualquer: comparar com ele quando o e-mail não existe deixa o tempo de resposta igual. */
    private final String hashFalso;

    private final GoogleTokenVerifier google;
    private final TokenService tokens;
    private final UsuarioRepository usuarios;
    private final CodigoService codigos;
    private final PasswordEncoder senhas;
    private final AppProperties.Auth config;
    private final Clock relogio;

    public AuthService(GoogleTokenVerifier google, TokenService tokens, UsuarioRepository usuarios, CodigoService codigos,
                       PasswordEncoder senhas, AppProperties propriedades, Clock relogio) {
        this.google = google;
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.senhas = senhas;
        this.config = propriedades.auth();
        this.relogio = relogio;
        this.hashFalso = senhas.encode("senha-que-ninguem-usa");
    }

    // ---- Cadastro -----------------------------------------------------------------------

    public Pedidos.Aviso cadastrar(Pedidos.Cadastro pedido) {
        String email = normalizar(pedido.email());
        exigirDominioPermitido(email);

        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);
        if (usuario != null && usuario.isEmailVerificado()) {
            throw new RegraNegocioException(
                    "Já existe uma conta com este e-mail. Entre com a sua senha ou use \"Esqueci a senha\".");
        }
        if (usuario == null) {
            usuario = new Usuario(pedido.nome().trim(), email, papelInicial(email), relogio.instant());
        } else {
            usuario.atualizarPerfil(pedido.nome().trim(), usuario.getFotoUrl());
        }
        usuario.definirSenhaHash(senhas.encode(pedido.senha()));
        usuarios.save(usuario);

        codigos.enviar(usuario, Finalidade.CADASTRO);
        return new Pedidos.Aviso("Enviamos um código de 6 números para " + email + ".");
    }

    public Login confirmarCadastro(Pedidos.Confirmar pedido) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(normalizar(pedido.email()))
                .orElseThrow(() -> new CodigoInvalidoException("Código incorreto."));
        if (usuario.isEmailVerificado()) {
            throw new RegraNegocioException("Este e-mail já foi confirmado. Entre com a sua senha.");
        }
        codigos.validar(usuario, Finalidade.CADASTRO, pedido.codigo());
        usuario.confirmarEmail();
        return concluirLogin(usuario);
    }

    // ---- Entrar -------------------------------------------------------------------------

    public Login entrar(Pedidos.Entrar pedido) {
        Optional<Usuario> encontrado = usuarios.findByEmailIgnoreCase(normalizar(pedido.email()));
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(null);

        // Mesma mensagem (e mesmo tempo) para e-mail inexistente e senha errada:
        // assim ninguém descobre quais e-mails têm conta.
        boolean confere = senhas.matches(pedido.senha(), hash != null ? hash : hashFalso) && hash != null;
        if (!confere) {
            throw new LoginInvalidoException("E-mail ou senha incorretos.");
        }
        Usuario usuario = encontrado.get();
        if (!usuario.isEmailVerificado()) {
            codigos.enviar(usuario, Finalidade.CADASTRO);
            throw new EmailNaoVerificadoException();
        }
        return concluirLogin(usuario);
    }

    // ---- Códigos e senha ----------------------------------------------------------------

    /** Reenvia o código. Responde igual exista ou não a conta. */
    public Pedidos.Aviso reenviarCodigo(Pedidos.Reenviar pedido) {
        usuarios.findByEmailIgnoreCase(normalizar(pedido.email()))
                .filter(Usuario::isAtivo)
                .filter(u -> pedido.finalidade() == Finalidade.CADASTRO ? !u.isEmailVerificado() : u.isEmailVerificado())
                .ifPresent(u -> codigos.enviar(u, pedido.finalidade()));
        return new Pedidos.Aviso("Se houver uma conta com este e-mail, um código novo chega em instantes.");
    }

    public Pedidos.Aviso esqueciSenha(Pedidos.SoEmail pedido) {
        usuarios.findByEmailIgnoreCase(normalizar(pedido.email()))
                .filter(Usuario::isAtivo)
                .filter(Usuario::isEmailVerificado)
                .ifPresent(u -> codigos.enviar(u, Finalidade.REDEFINIR_SENHA));
        return new Pedidos.Aviso("Se houver uma conta com este e-mail, o código chega em instantes.");
    }

    public Login redefinirSenha(Pedidos.RedefinirSenha pedido) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(normalizar(pedido.email()))
                .orElseThrow(() -> new CodigoInvalidoException("Código incorreto."));
        codigos.validar(usuario, Finalidade.REDEFINIR_SENHA, pedido.codigo());
        usuario.definirSenhaHash(senhas.encode(pedido.novaSenha()));
        // Trocou a senha: quem estava logado com a senha antiga (em qualquer aparelho) sai.
        usuario.encerrarSessoes(relogio.instant());
        return concluirLogin(usuario);
    }

    // ---- Google (opcional, só aparece quando configurado) ---------------------------------

    public Login entrarComGoogle(String idToken) {
        IdentidadeGoogle identidade = google.verificar(idToken);
        if (!identidade.emailVerificado()) {
            throw new LoginInvalidoException("Seu e-mail do Google ainda não foi verificado.");
        }
        String email = normalizar(identidade.email());
        if (!dominioPermitido(email)) {
            throw new LoginInvalidoException(mensagemDominio());
        }
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseGet(() -> new Usuario(nomeOuEmail(identidade), email, papelInicial(email), relogio.instant()));
        usuario.atualizarPerfil(identidade.nome(), identidade.fotoUrl());
        usuario.confirmarEmail();
        return concluirLogin(usuario);
    }

    /** Sair encerra a sessão de verdade: o token antigo para de valer mesmo que alguém o tenha copiado. */
    public void sair(Long usuarioId) {
        usuarios.findById(usuarioId).ifPresent(u -> u.encerrarSessoes(relogio.instant()));
    }

    public List<String> dominiosPermitidos() {
        return config.dominiosPermitidos();
    }

    // ---- Apoio --------------------------------------------------------------------------

    private Login concluirLogin(Usuario usuario) {
        if (!usuario.isAtivo()) {
            throw new LoginInvalidoException("Sua conta foi desativada. Fale com a coordenação da monitoria.");
        }
        if (ehAdminConfigurado(usuario.getEmail()) && !usuario.isAdmin()) {
            usuario.alterarPapel(Papel.ADMIN);
        }
        usuarios.save(usuario);
        return new Login(UsuarioDTO.de(usuario), tokens.emitir(usuario));
    }

    private void exigirDominioPermitido(String email) {
        if (!dominioPermitido(email)) {
            throw new RegraNegocioException(mensagemDominio());
        }
    }

    private boolean dominioPermitido(String email) {
        List<String> dominios = config.dominiosPermitidos();
        if (dominios.isEmpty()) {
            return true;
        }
        String dominio = email.substring(email.indexOf('@') + 1);
        return dominios.stream()
                .map(d -> d.trim().toLowerCase(Locale.ROOT))
                .anyMatch(dominio::equals);
    }

    private String mensagemDominio() {
        return "Use o seu e-mail institucional (@" + String.join(" ou @", config.dominiosPermitidos()) + ").";
    }

    private Papel papelInicial(String email) {
        return ehAdminConfigurado(email) ? Papel.ADMIN : Papel.ALUNO;
    }

    private boolean ehAdminConfigurado(String email) {
        return config.adminEmails().stream().anyMatch(admin -> admin.trim().equalsIgnoreCase(email));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String nomeOuEmail(IdentidadeGoogle identidade) {
        return identidade.nome() != null && !identidade.nome().isBlank() ? identidade.nome() : identidade.email();
    }

    /** Resultado interno do login: os dados para a resposta e o token para o cookie. */
    public record Login(UsuarioDTO usuario, TokenService.TokenEmitido token) {
    }
}
