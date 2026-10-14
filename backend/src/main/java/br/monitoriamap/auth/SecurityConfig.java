package br.monitoriamap.auth;

import br.monitoriamap.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Regras de segurança da API.
 *
 * <ul>
 *   <li><b>Sessão em cookie HttpOnly</b> ({@link CookieDeSessao}, lido pelo {@link FiltroDeSessao}):
 *       o token não fica acessível ao JavaScript.</li>
 *   <li><b>CSRF</b> ({@link CsrfParaSpa}): como o cookie vai sozinho, todo POST/PATCH precisa provar
 *       que veio da nossa página, devolvendo o valor do cookie {@code XSRF-TOKEN} num cabeçalho.</li>
 *   <li><b>Limite de tentativas</b> ({@link LimiteDeTentativas}) no login e na abertura de pedidos.</li>
 *   <li><b>Erros em JSON</b>: 401 e 403 voltam no mesmo formato ProblemDetail do resto da API.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, FiltroDeSessao sessao, LimiteDeTentativas limite,
                                       ObjectMapper json) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(repositorioCsrf())
                        .csrfTokenRequestHandler(new CsrfParaSpa()))
                .cors(withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .headers(h -> h
                        // A API só devolve JSON: nenhuma resposta dela pode carregar scripts ou ser embutida.
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                req -> req.getRequestURI().startsWith("/api/"),
                                new StaticHeadersWriter("Content-Security-Policy",
                                        "default-src 'none'; frame-ancestors 'none'")))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(regras -> regras
                        // O SSE completa a resposta num "async dispatch", que já foi autorizado na entrada.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/actuator/health",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, resp, e) -> erro(resp, json, HttpStatus.UNAUTHORIZED,
                                "Sessão encerrada", "Entre novamente para continuar."))
                        .accessDeniedHandler((req, resp, e) -> negado(resp, json, e)))
                .addFilterBefore(limite, CsrfFilter.class)
                .addFilterBefore(sessao, AnonymousAuthenticationFilter.class);
        return http.build();
    }

    /** Os dois filtros já rodam dentro da cadeia do Spring Security; isto evita que o Boot os registre de novo. */
    @Bean
    public FilterRegistrationBean<LimiteDeTentativas> naoRegistrarLimiteDuasVezes(LimiteDeTentativas filtro) {
        FilterRegistrationBean<LimiteDeTentativas> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<FiltroDeSessao> naoRegistrarSessaoDuasVezes(FiltroDeSessao filtro) {
        FilterRegistrationBean<FiltroDeSessao> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    /** Cookie XSRF-TOKEN legível pelo JavaScript (é o objetivo), mas restrito ao mesmo site. */
    private static CookieCsrfTokenRepository repositorioCsrf() {
        CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repositorio.setCookiePath("/");
        repositorio.setCookieCustomizer(c -> c.sameSite("Strict"));
        return repositorio;
    }

    private static void negado(HttpServletResponse resp, ObjectMapper json, Exception e) throws IOException {
        if (e instanceof CsrfException) {
            erro(resp, json, HttpStatus.FORBIDDEN, "Página desatualizada",
                    "Recarregue a página e tente de novo.");
        } else {
            erro(resp, json, HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para esta ação.");
        }
    }

    private static void erro(HttpServletResponse resp, ObjectMapper json, HttpStatus status, String titulo,
                             String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        resp.setStatus(status.value());
        resp.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        json.writeValue(resp.getOutputStream(), problema);
    }

    /**
     * Em produção o front e a API ficam na mesma origem (proxy do nginx ou rewrite da Vercel),
     * então o CORS só importa no desenvolvimento. A lista de origens é sempre explícita.
     */
    @Bean
    public CorsConfigurationSource cors(AppProperties propriedades) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(propriedades.cors().origens());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", config);
        return fonte;
    }
}
