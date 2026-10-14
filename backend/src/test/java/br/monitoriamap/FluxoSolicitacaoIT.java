package br.monitoriamap;

import br.monitoriamap.auth.codigo.EnvioDeCodigo;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.Instant;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração de ponta a ponta: Spring completo + PostgreSQL real em um container.
 * Precisa do Docker rodando. Execute com {@code mvn verify}.
 *
 * <p>A sessão viaja no cookie {@code monitoria_sessao} e todo POST leva o token CSRF
 * ({@code .with(csrf())}). O envio de códigos por e-mail é substituído por um dublê.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "app.limites.novas-solicitacoes-por-hora=1000",
        "app.limites.tentativas-de-login-por-minuto=1000"
})
class FluxoSolicitacaoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    PasswordEncoder senhas;

    /** Substitui o envio por e-mail: o teste lê o código direto daqui. */
    @MockitoBean
    EnvioDeCodigo envioDeCodigo;

    private static final String SENHA = "senha-de-teste-123";

    private Cookie aluno;
    private Cookie ana;
    private Cookie bruno;

    @BeforeEach
    void entrar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        aluno = login("aluno-" + sufixo + "@exemplo.com", "Maria Aluna", "ALUNO");
        ana = login("ana-" + sufixo + "@exemplo.com", "Ana Monitora", "MONITOR");
        bruno = login("bruno-" + sufixo + "@exemplo.com", "Bruno Monitor", "MONITOR");
    }

    @Test
    void cicloCompletoDeUmaSolicitacao() throws Exception {
        long id = abrirSolicitacao(aluno);

        mvc.perform(get("/api/solicitacoes").cookie(aluno))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/solicitacoes/{id}/assumir", id).cookie(ana).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.monitor.nome").value("Ana Monitora"));

        mvc.perform(post("/api/solicitacoes/{id}/assumir", id).cookie(bruno).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta solicitação já foi assumida por Ana Monitora."));

        mvc.perform(post("/api/solicitacoes/{id}/concluir", id).cookie(ana).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Resolvido com um exemplo.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        mvc.perform(get("/api/solicitacoes/{id}", id).cookie(aluno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historico.length()").value(3))
                .andExpect(jsonPath("$.historico[2].tipo").value("CONCLUIDA"));
    }

    @Test
    void validacaoDevolveMensagemAmigavel() throws Exception {
        mvc.perform(post("/api/solicitacoes").cookie(aluno).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicoId\":1,\"titulo\":\"\",\"descricao\":\"x\",\"modalidade\":\"ONLINE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.titulo").value("Dê um título para a sua dúvida."));
    }

    // ---- Segurança ---------------------------------------------------------------------

    @Test
    void semSessaoRecebe401EmJson() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Entre novamente para continuar."));
    }

    @Test
    void sessaoRespondeSemErroParaVisitanteEComNomeParaQuemEntrou() throws Exception {
        mvc.perform(get("/api/auth/sessao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").doesNotExist());
        mvc.perform(get("/api/auth/sessao").cookie(aluno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.nome").value("Maria Aluna"));
    }

    @Test
    void loginEntregaCookieHttpOnlyENaoDevolveOToken() throws Exception {
        String email = "x-" + UUID.randomUUID() + "@exemplo.com";
        login(email, "Xis", "ALUNO");
        entrar(email, SENHA)
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("monitoria_sessao", true))
                .andExpect(cookie().path("monitoria_sessao", "/api"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.usuario.nome").value("Xis"));
    }

    @Test
    void postSemTokenCsrfEhRecusado() throws Exception {
        mvc.perform(post("/api/solicitacoes").cookie(aluno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicoId\":1,\"titulo\":\"t\",\"descricao\":\"d\",\"modalidade\":\"ONLINE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Página desatualizada"));
    }

    @Test
    void depoisDeSairOCookieAntigoNaoValeMais() throws Exception {
        mvc.perform(get("/api/me").cookie(bruno)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/sair").cookie(bruno).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("monitoria_sessao", 0));

        // Mesmo que alguém tenha copiado o cookie, ele foi invalidado no servidor.
        mvc.perform(get("/api/me").cookie(bruno)).andExpect(status().isUnauthorized());
    }

    @Test
    void respostasNaoExpoemEmailDeOutrasPessoas() throws Exception {
        long id = abrirSolicitacao(aluno);
        mvc.perform(post("/api/solicitacoes/{id}/assumir", id).cookie(ana).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("@exemplo.com"))));
        mvc.perform(get("/api/solicitacoes?status=EM_ATENDIMENTO").cookie(ana))
                .andExpect(content().string(not(containsString("@exemplo.com"))));
    }

    @Test
    void alunoNaoVeSolicitacaoDeOutroAluno() throws Exception {
        long id = abrirSolicitacao(aluno);
        Cookie outro = login("outro-" + UUID.randomUUID() + "@exemplo.com", "Outro Aluno", "ALUNO");
        mvc.perform(get("/api/solicitacoes/{id}", id).cookie(outro))
                .andExpect(status().isNotFound());
    }

    @Test
    void alunoNaoPodeLotarAFila() throws Exception {
        abrirSolicitacao(aluno);
        abrirSolicitacao(aluno);
        mvc.perform(post("/api/solicitacoes").cookie(aluno).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicoId\":1,\"titulo\":\"t\",\"descricao\":\"d\",\"modalidade\":\"PRESENCIAL\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Dez monitores clicam em "Assumir" ao mesmo tempo: exatamente um consegue,
     * os outros recebem 409. Isso prova que o lock otimista (@Version) funciona.
     */
    @Test
    void apenasUmMonitorConsegueAssumirQuandoClicamJuntos() throws Exception {
        long id = abrirSolicitacao(aluno);
        int quantidade = 10;
        List<Cookie> monitores = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            monitores.add(login("monitor" + i + "-" + UUID.randomUUID() + "@exemplo.com", "Monitor " + i, "MONITOR"));
        }

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(quantidade);
        List<Future<Integer>> respostas = new ArrayList<>();
        for (Cookie monitor : monitores) {
            Callable<Integer> tentativa = () -> {
                largada.await();
                return mvc.perform(post("/api/solicitacoes/{id}/assumir", id).cookie(monitor).with(csrf()))
                        .andReturn().getResponse().getStatus();
            };
            respostas.add(threads.submit(tentativa));
        }
        largada.countDown();

        List<Integer> status = new ArrayList<>();
        for (Future<Integer> resposta : respostas) {
            status.add(resposta.get());
        }
        threads.shutdown();

        assertThat(status).filteredOn(s -> s == 200).hasSize(1);
        assertThat(status).filteredOn(s -> s == 409).hasSize(quantidade - 1);
    }

    private long abrirSolicitacao(Cookie quem) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/solicitacoes").cookie(quem).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicoId": 1, "titulo": "Dúvida no Observer",
                                 "descricao": "Não entendi quando chamar notify.", "modalidade": "PRESENCIAL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    /** Cria a conta já confirmada direto no banco e entra pela API com e-mail e senha. */
    private Cookie login(String email, String nome, String papel) throws Exception {
        Usuario usuario = new Usuario(nome, email, Papel.valueOf(papel), Instant.now());
        usuario.definirSenhaHash(senhas.encode(SENHA));
        usuario.confirmarEmail();
        usuarios.save(usuario);
        return entrar(email, SENHA).andExpect(status().isOk()).andReturn().getResponse().getCookie("monitoria_sessao");
    }

    private org.springframework.test.web.servlet.ResultActions entrar(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/entrar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "senha", senha))));
    }

    private org.springframework.test.web.servlet.ResultActions postar(String caminho, Object corpo) throws Exception {
        return mvc.perform(post(caminho).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corpo)));
    }

    /** Pega o último código que o sistema "enviou" para esta pessoa (o envio real é substituído no teste). */
    private String ultimoCodigo(String email) {
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(envioDeCodigo, atLeastOnce()).enviar(argThat(u -> u.getEmail().equals(email)), any(), codigo.capture());
        return codigo.getValue();
    }

    // ---- Conta: cadastro com e-mail institucional, código e senha -----------------------------

    @Test
    void cadastroSoAceitaEmailInstitucional() throws Exception {
        postar("/api/auth/cadastro", Map.of("nome", "Zé", "email", "ze@gmail.com", "senha", "senha-forte-123"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("@aluno.uepb.edu.br")));
    }

    @Test
    void cadastroCompletoComCodigoPorEmail() throws Exception {
        String email = "joana." + UUID.randomUUID().toString().substring(0, 6) + "@aluno.uepb.edu.br";
        postar("/api/auth/cadastro", Map.of("nome", "Joana Lima", "email", email, "senha", "minha-senha-1"))
                .andExpect(status().isAccepted());

        // Antes de confirmar o e-mail, a senha certa não basta: pede o código.
        entrar(email, "minha-senha-1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("email-nao-verificado"));

        postar("/api/auth/confirmar", Map.of("email", email, "codigo", "000000"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("Código incorreto")));

        postar("/api/auth/confirmar", Map.of("email", email, "codigo", ultimoCodigo(email)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("monitoria_sessao", true))
                .andExpect(jsonPath("$.usuario.papel").value("ALUNO"));

        entrar(email, "minha-senha-1").andExpect(status().isOk());
    }

    @Test
    void codigoAceitaNoMaximoCincoTentativas() throws Exception {
        String email = "chute." + UUID.randomUUID().toString().substring(0, 6) + "@aluno.uepb.edu.br";
        postar("/api/auth/cadastro", Map.of("nome", "Chute", "email", email, "senha", "minha-senha-1"));
        String certo = ultimoCodigo(email);
        String errado = certo.equals("111111") ? "222222" : "111111";
        for (int i = 0; i < 5; i++) {
            postar("/api/auth/confirmar", Map.of("email", email, "codigo", errado));
        }
        // Mesmo acertando agora, o código já foi queimado.
        postar("/api/auth/confirmar", Map.of("email", email, "codigo", certo))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void senhaErradaEEmailInexistenteDaoAMesmaResposta() throws Exception {
        login("rita-" + UUID.randomUUID() + "@exemplo.com", "Rita", "ALUNO");
        entrar("ninguem@aluno.uepb.edu.br", "qualquer-coisa")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
        entrar("ninguem@aluno.uepb.edu.br", "outra-coisa")
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void esqueciASenhaTrocaASenhaEDerrubaSessoesAntigas() throws Exception {
        String email = "lucas-" + UUID.randomUUID() + "@exemplo.com";
        Cookie antigo = login(email, "Lucas", "ALUNO");

        postar("/api/auth/esqueci-senha", Map.of("email", email)).andExpect(status().isAccepted());
        postar("/api/auth/redefinir-senha", Map.of("email", email, "codigo", ultimoCodigo(email), "novaSenha", "senha-nova-99"))
                .andExpect(status().isOk());

        entrar(email, SENHA).andExpect(status().isUnauthorized());
        entrar(email, "senha-nova-99").andExpect(status().isOk());
        mvc.perform(get("/api/me").cookie(antigo)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginDeDesenvolvimentoNaoExisteMais() throws Exception {
        postar("/api/auth/dev", Map.of("email", "x@exemplo.com", "nome", "X", "papel", "ADMIN"))
                .andExpect(status().is4xxClientError());
    }

    // ---- Link da reunião -------------------------------------------------------------------

    @Test
    void monitorAdicionaOLinkDepoisDeAssumir() throws Exception {
        MvcResult criado = mvc.perform(post("/api/solicitacoes").cookie(aluno).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicoId\":1,\"titulo\":\"t\",\"descricao\":\"d\",\"modalidade\":\"ONLINE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linkReuniao").doesNotExist())
                .andReturn();
        long id = json.readTree(criado.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/solicitacoes/{id}/assumir", id).cookie(ana).with(csrf())).andExpect(status().isOk());

        mvc.perform(patch("/api/solicitacoes/{id}/link", id).cookie(bruno).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"linkReuniao\":\"https://meet.google.com/aaa\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/solicitacoes/{id}/link", id).cookie(ana).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"linkReuniao\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/solicitacoes/{id}/link", id).cookie(ana).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"linkReuniao\":\"https://meet.google.com/abc-defg-hij\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkReuniao").value("https://meet.google.com/abc-defg-hij"))
                .andExpect(jsonPath("$.historico[2].tipo").value("LINK_DEFINIDO"));
    }
}
