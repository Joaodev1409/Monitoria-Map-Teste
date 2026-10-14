package br.monitoriamap.auth;

import br.monitoriamap.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Onde o token da sessão mora: um cookie que o navegador guarda e envia sozinho.
 *
 * <ul>
 *   <li>{@code HttpOnly}: o JavaScript da página não consegue ler o token. Ele não aparece em
 *       Local Storage e um script malicioso (XSS) não tem como roubá-lo.</li>
 *   <li>{@code Secure}: só trafega em HTTPS.</li>
 *   <li>{@code SameSite=Lax}: outros sites não conseguem disparar POSTs com o cookie
 *       (além disso, todo POST exige o token CSRF; veja {@link SecurityConfig}).</li>
 *   <li>{@code Path=/api}: o cookie só vai para a API, nunca para os arquivos do site.</li>
 * </ul>
 */
@Component
public class CookieDeSessao {

    private final AppProperties.Sessao config;
    private final Clock relogio;

    public CookieDeSessao(AppProperties propriedades, Clock relogio) {
        this.config = propriedades.sessao();
        this.relogio = relogio;
    }

    public ResponseCookie criar(TokenService.TokenEmitido token) {
        Duration duracao = Duration.between(relogio.instant(), token.expiraEm());
        return base(token.token()).maxAge(duracao).build();
    }

    public ResponseCookie apagar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public String ler(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (config.nomeCookie().equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(config.nomeCookie(), valor)
                .httpOnly(true)
                .secure(config.cookieSeguro())
                .sameSite("Lax")
                .path("/api");
    }
}
