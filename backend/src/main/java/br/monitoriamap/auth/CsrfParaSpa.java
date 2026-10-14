package br.monitoriamap.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Proteção CSRF para uma SPA (receita da documentação do Spring Security).
 *
 * <p>Como a sessão agora é um cookie, o navegador o envia sozinho em qualquer requisição,
 * inclusive numa disparada por outro site. Para impedir isso, o servidor grava um cookie
 * {@code XSRF-TOKEN} (legível pelo JavaScript) e exige que todo POST/PATCH devolva o mesmo
 * valor no cabeçalho {@code X-XSRF-TOKEN}. Um site estranho não consegue ler o cookie,
 * então não consegue montar o cabeçalho.
 */
final class CsrfParaSpa implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler simples = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler mascarado = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> token) {
        mascarado.handle(request, response, token);
        // Carrega o token já na primeira requisição, para o cookie XSRF-TOKEN ser gravado.
        token.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
        String cabecalho = request.getHeader(token.getHeaderName());
        return (StringUtils.hasText(cabecalho) ? simples : mascarado).resolveCsrfTokenValue(request, token);
    }
}
