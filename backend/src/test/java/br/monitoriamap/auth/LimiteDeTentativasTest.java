package br.monitoriamap.auth;

import br.monitoriamap.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** O freio contra abuso: login e pedidos por IP, mensagens por sessão. */
class LimiteDeTentativasTest {

    private final Clock relogio = Clock.fixed(Instant.parse("2026-10-14T13:00:00Z"), ZoneOffset.UTC);
    private final AppProperties propriedades = new AppProperties(
            null,
            new AppProperties.Sessao("x".repeat(32), Duration.ofHours(8), "monitoria_sessao", true),
            null, null,
            new AppProperties.Limites(2, 3, 2, 2),
            "http://localhost", ZoneId.of("America/Fortaleza"));
    private final LimiteDeTentativas limite =
            new LimiteDeTentativas(propriedades, new CookieDeSessao(propriedades, relogio), new ObjectMapper(), relogio);

    private int status(String metodo, String caminho, String ip, String sessao) throws Exception {
        MockHttpServletRequest pedido = new MockHttpServletRequest(metodo, caminho);
        pedido.setRemoteAddr(ip);
        if (sessao != null) {
            pedido.setCookies(new Cookie("monitoria_sessao", sessao));
        }
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        limite.doFilter(pedido, resposta, new MockFilterChain());
        return resposta.getStatus();
    }

    @Test
    void loginEhLimitadoPorIp() throws Exception {
        List<Integer> respostas = List.of(
                status("POST", "/api/auth/google", "10.0.0.1", null),
                status("POST", "/api/auth/google", "10.0.0.1", null),
                status("POST", "/api/auth/google", "10.0.0.1", null),
                status("POST", "/api/auth/google", "10.0.0.1", null));

        assertThat(respostas).containsExactly(200, 200, 200, 429);
        assertThat(status("POST", "/api/auth/google", "10.0.0.2", null)).as("outro IP").isEqualTo(200);
    }

    @Test
    void mensagensSaoLimitadasPorSessaoENaoPorIp() throws Exception {
        String ipDaSala = "10.0.0.9";
        assertThat(status("POST", "/api/solicitacoes/5/mensagens", ipDaSala, "sessao-da-maria")).isEqualTo(200);
        assertThat(status("POST", "/api/solicitacoes/5/mensagens", ipDaSala, "sessao-da-maria")).isEqualTo(200);
        assertThat(status("POST", "/api/solicitacoes/5/mensagens", ipDaSala, "sessao-da-maria")).isEqualTo(429);

        assertThat(status("POST", "/api/solicitacoes/8/mensagens", ipDaSala, "sessao-do-joao"))
                .as("colega na mesma rede não é barrado")
                .isEqualTo(200);
    }

    @Test
    void leiturasEOutrasRotasNaoSaoLimitadas() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(status("GET", "/api/solicitacoes/5/mensagens", "10.0.0.3", "s")).isEqualTo(200);
            assertThat(status("POST", "/api/solicitacoes/5/assumir", "10.0.0.3", "s")).isEqualTo(200);
        }
    }
}
