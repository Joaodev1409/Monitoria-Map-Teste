package br.monitoriamap.auth;

import br.monitoriamap.auth.dto.LoginGoogleRequest;
import br.monitoriamap.auth.dto.LoginResponse;
import br.monitoriamap.auth.dto.Pedidos;
import br.monitoriamap.config.AppProperties;
import br.monitoriamap.usuario.UsuarioDTO;
import br.monitoriamap.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final CookieDeSessao cookie;
    private final UsuarioService usuarios;
    private final AppProperties.Auth config;

    public AuthController(AuthService auth, CookieDeSessao cookie, UsuarioService usuarios, AppProperties propriedades) {
        this.auth = auth;
        this.cookie = cookie;
        this.usuarios = usuarios;
        this.config = propriedades.auth();
    }

    /** Configuração pública que a tela de login lê (domínios aceitos e se o Google está ligado). */
    @GetMapping("/config")
    public ConfiguracaoLogin configuracao() {
        return new ConfiguracaoLogin(config.googleClientId(), auth.dominiosPermitidos());
    }

    /**
     * Quem está logado, se alguém estiver. Sempre responde 200 (com {@code usuario: null} para
     * visitantes): o front descobre a sessão sem gerar erros 401 no console a cada visita.
     */
    @GetMapping("/sessao")
    public Sessao sessao(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return new Sessao(usuario == null ? null : usuarios.buscar(usuario.id()));
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Pedidos.Aviso cadastrar(@Valid @RequestBody Pedidos.Cadastro pedido) {
        return auth.cadastrar(pedido);
    }

    @PostMapping("/confirmar")
    public ResponseEntity<LoginResponse> confirmar(@Valid @RequestBody Pedidos.Confirmar pedido) {
        return responder(auth.confirmarCadastro(pedido));
    }

    @PostMapping("/entrar")
    public ResponseEntity<LoginResponse> entrar(@Valid @RequestBody Pedidos.Entrar pedido) {
        return responder(auth.entrar(pedido));
    }

    @PostMapping("/reenviar-codigo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Pedidos.Aviso reenviarCodigo(@Valid @RequestBody Pedidos.Reenviar pedido) {
        return auth.reenviarCodigo(pedido);
    }

    @PostMapping("/esqueci-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Pedidos.Aviso esqueciSenha(@Valid @RequestBody Pedidos.SoEmail pedido) {
        return auth.esqueciSenha(pedido);
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<LoginResponse> redefinirSenha(@Valid @RequestBody Pedidos.RedefinirSenha pedido) {
        return responder(auth.redefinirSenha(pedido));
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> entrarComGoogle(@Valid @RequestBody LoginGoogleRequest pedido) {
        return responder(auth.entrarComGoogle(pedido.credential()));
    }

    @PostMapping("/sair")
    public ResponseEntity<Void> sair(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (usuario != null) {
            auth.sair(usuario.id());
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.apagar().toString()).build();
    }

    private ResponseEntity<LoginResponse> responder(AuthService.Login login) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.criar(login.token()).toString())
                .body(new LoginResponse(login.usuario(), login.token().expiraEm()));
    }

    public record Sessao(UsuarioDTO usuario) {
    }

    public record ConfiguracaoLogin(String googleClientId, List<String> dominiosPermitidos) {
    }
}
