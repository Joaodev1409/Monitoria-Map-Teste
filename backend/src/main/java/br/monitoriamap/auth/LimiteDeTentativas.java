package br.monitoriamap.auth;

import br.monitoriamap.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite simples de requisições por IP (janela fixa), em memória.
 *
 * <ul>
 *   <li>Login: no máximo N tentativas por minuto. Freia robôs testando credenciais.</li>
 *   <li>Abrir solicitação: no máximo N por hora. Impede encher a fila (e a caixa de
 *       e-mail dos monitores) com pedidos em série.</li>
 * </ul>
 *
 * Para uma instância só, isto basta. Com várias instâncias, o contador teria de ir para
 * um armazenamento compartilhado (ex.: Redis).
 */
@Component
public class LimiteDeTentativas extends OncePerRequestFilter {

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final AppProperties.Limites limites;
    private final ObjectMapper json;
    private final Clock relogio;

    public LimiteDeTentativas(AppProperties propriedades, ObjectMapper json, Clock relogio) {
        this.limites = propriedades.limites();
        this.json = json;
        this.relogio = relogio;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || regra(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Regra regra = regra(request);
        String chave = regra.nome() + "|" + request.getRemoteAddr();
        long agora = relogio.millis();
        Janela janela = janelas.compute(chave, (k, atual) ->
                atual == null || agora - atual.inicio() >= regra.duracao().toMillis() ? new Janela(agora) : atual);

        if (janela.contador().incrementAndGet() > regra.maximo()) {
            long segundos = Math.max(1, (janela.inicio() + regra.duracao().toMillis() - agora) / 1000);
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, regra.mensagem());
            problema.setTitle("Muitas tentativas");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(segundos));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            json.writeValue(response.getOutputStream(), problema);
            return;
        }
        chain.doFilter(request, response);
    }

    /** Remove janelas vencidas para o mapa não crescer para sempre. */
    @Scheduled(fixedRate = 300_000)
    void limpar() {
        long agora = relogio.millis();
        janelas.values().removeIf(j -> agora - j.inicio() > Duration.ofHours(1).toMillis());
    }

    private Regra regra(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        if (caminho.startsWith("/api/auth/") && !caminho.equals("/api/auth/sair")) {
            return new Regra("login", limites.tentativasDeLoginPorMinuto(), Duration.ofMinutes(1),
                    "Muitas tentativas de login. Espere um minuto e tente de novo.");
        }
        if (caminho.equals("/api/solicitacoes")) {
            return new Regra("solicitacao", limites.novasSolicitacoesPorHora(), Duration.ofHours(1),
                    "Você abriu muitas solicitações em pouco tempo. Espere um pouco antes de abrir outra.");
        }
        return null;
    }

    private record Regra(String nome, int maximo, Duration duracao, String mensagem) {
    }

    private record Janela(long inicio, AtomicInteger contador) {
        Janela(long inicio) {
            this(inicio, new AtomicInteger());
        }
    }
}
