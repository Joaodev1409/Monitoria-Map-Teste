package br.monitoriamap.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê o cookie de sessão, valida o JWT e coloca o usuário no contexto de segurança.
 *
 * <p>Por que um filtro próprio em vez do {@code oauth2ResourceServer()} do Spring? Aquele
 * módulo foi feito para tokens no cabeçalho {@code Authorization} e, por isso, desliga a
 * proteção CSRF para requisições que trazem token. Com o token num cookie (que o navegador
 * envia sozinho), isso abriria a porta para CSRF. Aqui o CSRF continua valendo para tudo.
 *
 * <p>Um cookie inválido ou vencido não derruba a requisição: ela segue como anônima, e as
 * rotas protegidas respondem 401 normalmente. Assim, uma sessão velha nunca impede um login novo.
 */
@Component
public class FiltroDeSessao extends OncePerRequestFilter {

    private final CookieDeSessao cookie;
    private final TokenService tokens;
    private final UsuarioAuthenticationConverter conversor;

    public FiltroDeSessao(CookieDeSessao cookie, TokenService tokens, UsuarioAuthenticationConverter conversor) {
        this.cookie = cookie;
        this.tokens = tokens;
        this.conversor = conversor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = cookie.ler(request);
        if (token != null) {
            try {
                Jwt jwt = tokens.decoder().decode(token);
                AbstractAuthenticationToken autenticacao = conversor.convert(jwt);
                SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(autenticacao);
                SecurityContextHolder.setContext(contexto);
            } catch (JwtException | InvalidBearerTokenException e) {
                // Sessão vencida, adulterada ou encerrada: segue como visitante anônimo.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
