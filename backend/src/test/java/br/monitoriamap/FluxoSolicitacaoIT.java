package br.monitoriamap;

import br.monitoriamap.auth.GoogleTokenVerifier;
import br.monitoriamap.auth.GoogleTokenVerifier.IdentidadeGoogle;
import br.monitoriamap.auth.TokenService;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.Instant;
import static org.mockito.Mockito.when;
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
 * ({@code .with(csrf())}). A verificação do token do Google é substituída por um dublê,
 * para simular contas da UEPB, contas pessoais e de outros domínios sem internet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "app.limites.novas-solicitacoes-por-hora=1000",
        "app.limites.tentativas-de-login-por-minuto=1000",
        "app.limites.mensagens-por-minuto=1000"
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
    TokenService tokens;

    /** Simula o Google: cada teste diz que identidade o "token" representa. */
    @MockitoBean
    GoogleTokenVerifier google;

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
        String email = "xis." + UUID.randomUUID().toString().substring(0, 6) + "@aluno.uepb.edu.br";
        googleDiz("token-xis", email, "Xis", "aluno.uepb.edu.br");
        entrarComGoogle("token-xis")
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

    /** Cria a conta direto no banco e devolve um cookie de sessão válido para ela. */
    private Cookie login(String email, String nome, String papel) {
        Usuario usuario = usuarios.save(new Usuario(nome, email, Papel.valueOf(papel), Instant.now()));
        return new Cookie("monitoria_sessao", tokens.emitir(usuario).token());
    }

    private void googleDiz(String token, String email, String nome, String dominioGoogle) {
        when(google.verificar(token)).thenReturn(new IdentidadeGoogle(email, true, nome, null, dominioGoogle));
    }

    private org.springframework.test.web.servlet.ResultActions entrarComGoogle(String token) throws Exception {
        return mvc.perform(post("/api/auth/google").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("credential", token))));
    }

    // ---- Login: só conta Google da UEPB --------------------------------------------------------

    @Test
    void contaDaUepbEntraEViraAlunoNoPrimeiroAcesso() throws Exception {
        String email = "joana." + UUID.randomUUID().toString().substring(0, 6) + "@aluno.uepb.edu.br";
        googleDiz("token-joana", email, "Joana Lima", "aluno.uepb.edu.br");
        entrarComGoogle("token-joana")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.papel").value("ALUNO"))
                .andExpect(jsonPath("$.usuario.nome").value("Joana Lima"));
    }

    @Test
    void contaDeServidorTambemEntra() throws Exception {
        googleDiz("token-prof", "prof." + UUID.randomUUID().toString().substring(0, 6) + "@servidor.uepb.edu.br",
                "Professor", "servidor.uepb.edu.br");
        entrarComGoogle("token-prof").andExpect(status().isOk());
    }

    @Test
    void gmailPessoalEhRecusado() throws Exception {
        googleDiz("token-gmail", "ze@gmail.com", "Zé", null);
        entrarComGoogle("token-gmail")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(containsString("@aluno.uepb.edu.br")));
    }

    /** O domínio do e-mail sozinho não basta: o Google precisa dizer que a conta é da organização. */
    @Test
    void emailComDominioDaUepbMasSemContaDaOrganizacaoEhRecusado() throws Exception {
        googleDiz("token-falso", "falso@aluno.uepb.edu.br", "Falso", null);
        entrarComGoogle("token-falso").andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroComSenhaNaoExisteMais() throws Exception {
        mvc.perform(post("/api/auth/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"email\":\"x@aluno.uepb.edu.br\",\"senha\":\"12345678\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void loginDeDesenvolvimentoNaoExisteMais() throws Exception {
        mvc.perform(post("/api/auth/dev").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@exemplo.com\",\"nome\":\"X\",\"papel\":\"ADMIN\"}"))
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

    // ---- Conversa no pedido ------------------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions escrever(long id, Cookie quem, String texto)
            throws Exception {
        return mvc.perform(post("/api/solicitacoes/{id}/mensagens", id).cookie(quem).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("texto", texto))));
    }

    @Test
    void alunoEMonitorResponsavelConversamDuranteOAtendimento() throws Exception {
        long id = abrirSolicitacao(aluno);

        escrever(id, aluno, "Alguém aí?")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A conversa começa quando um monitor assumir o pedido."));

        mvc.perform(post("/api/solicitacoes/{id}/assumir", id).cookie(ana).with(csrf())).andExpect(status().isOk());

        escrever(id, aluno, "  Pode ser às 15h?  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Pode ser às 15h?"))
                .andExpect(jsonPath("$.autor.nome").value("Maria Aluna"))
                .andExpect(jsonPath("$.solicitacaoId").value(id));
        escrever(id, ana, "Pode! Manda o trecho:\n```java\nsujeito.notificar();\n```")
                .andExpect(status().isCreated());

        mvc.perform(get("/api/solicitacoes/{id}/mensagens", id).cookie(aluno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].texto").value("Pode ser às 15h?"))
                .andExpect(jsonPath("$[1].autor.nome").value("Ana Monitora"))
                .andExpect(jsonPath("$[1].texto").value(containsString("```java")))
                .andExpect(content().string(not(containsString("@exemplo.com"))));
        mvc.perform(get("/api/solicitacoes/{id}/mensagens", id).cookie(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void quemNaoParticipaNaoEntraNaConversa() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Cookie outroAluno = login("outro-" + sufixo + "@exemplo.com", "Outro Aluno", "ALUNO");
        Cookie admin = login("admin-" + sufixo + "@exemplo.com", "Prof Admin", "ADMIN");
        long id = abrirSolicitacao(aluno);
        mvc.perform(post("/api/solicitacoes/{id}/assumir", id).cookie(ana).with(csrf())).andExpect(status().isOk());
        escrever(id, aluno, "Oi, Ana!").andExpect(status().isCreated());

        // Outro aluno nem descobre que o pedido existe; outro monitor sabe, mas a conversa não é dele.
        mvc.perform(get("/api/solicitacoes/{id}/mensagens", id).cookie(outroAluno)).andExpect(status().isNotFound());
        escrever(id, outroAluno, "oi").andExpect(status().isNotFound());
        mvc.perform(get("/api/solicitacoes/{id}/mensagens", id).cookie(bruno)).andExpect(status().isForbidden());
        escrever(id, bruno, "oi").andExpect(status().isForbidden());

        // O admin acompanha, mas não escreve.
        mvc.perform(get("/api/solicitacoes/{id}/mensagens", id).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        escrever(id, admin, "oi")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Administradores acompanham a conversa, mas não escrevem nela."));
    }

    @Test
    void depoisDeConcluidoAConversaFicaSoParaLeitura() throws Exception {
        long id = abrirSolicitacao(aluno);
        mvc.perform(post("/api/solicitacoes/{id}/assumir", id).cookie(ana).with(csrf())).andExpect(status().isOk());
        escrever(id, ana, "Resolvido?").andExpect(status().isCreated());
        mvc.perform(post("/api/solicitacoes/{id}/concluir", id).cookie(ana).with(csrf())).andExpect(status().isOk());

        escrever(id, aluno, "Valeu!")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A conversa foi encerrada com o atendimento."));
        mvc.perform(get("/api/solicitacoes/{id}/mensagens", id).cookie(aluno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void mensagemInvalidaOuSemCsrfEhRecusada() throws Exception {
        long id = abrirSolicitacao(aluno);
        mvc.perform(post("/api/solicitacoes/{id}/assumir", id).cookie(ana).with(csrf())).andExpect(status().isOk());

        escrever(id, aluno, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.texto").value("Escreva a mensagem antes de enviar."));
        escrever(id, aluno, "a".repeat(4001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.texto").value("A mensagem pode ter até 4000 caracteres."));
        mvc.perform(post("/api/solicitacoes/{id}/mensagens", id).cookie(aluno)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"oi\"}"))
                .andExpect(status().isForbidden());
    }
}
