package br.monitoriamap.auth;

import br.monitoriamap.auth.dto.LoginGoogleRequest;
import br.monitoriamap.auth.dto.LoginResponse;
import br.monitoriamap.config.AppProperties;
import br.monitoriamap.usuario.UsuarioDTO;
import br.monitoriamap.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /** Configuração pública que a tela de login lê: o Client ID do Google e os domínios aceitos. */
    @GetMapping("/config")
    public ConfiguracaoLogin configuracao() {
        return new ConfiguracaoLogin(config.googleClientId(), auth.dominiosPermitidos(), config.contatoEmail());
    }

    /**
     * Quem está logado, se alguém estiver. Sempre responde 200 (com {@code usuario: null} para
     * visitantes): o front descobre a sessão sem gerar erros 401 no console a cada visita.
     */
    @GetMapping("/sessao")
    public Sessao sessao(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return new Sessao(usuario == null ? null : usuarios.buscar(usuario.id()));
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> entrarComGoogle(@Valid @RequestBody LoginGoogleRequest pedido) {
        AuthService.Login login = auth.entrarComGoogle(pedido.credential());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.criar(login.token()).toString())
                .body(new LoginResponse(login.usuario(), login.token().expiraEm()));
    }

    @PostMapping("/sair")
    public ResponseEntity<Void> sair(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (usuario != null) {
            auth.sair(usuario.id());
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.apagar().toString()).build();
    }

    public record Sessao(UsuarioDTO usuario) {
    }

    public record ConfiguracaoLogin(String googleClientId, List<String> dominiosPermitidos, String contatoEmail) {
    }
}
