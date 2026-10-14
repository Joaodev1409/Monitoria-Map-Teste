package br.monitoriamap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/**
 * Configurações próprias da aplicação (prefixo {@code app} no application.yml).
 * Agrupar tudo num record tipado evita {@code @Value("${...}")} espalhado pelo código.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue Auth auth,
        @DefaultValue Sessao sessao,
        @DefaultValue Cors cors,
        @DefaultValue Email email,
        @DefaultValue Limites limites,
        @DefaultValue("http://localhost:5173") String urlFrontend,
        /* Fuso usado para agrupar relatórios por dia/semana (a Paraíba é UTC-3). */
        @DefaultValue("America/Fortaleza") ZoneId fusoHorario
) {

    public record Auth(
            /* Client ID do Google (OAuth 2.0 → "Aplicativo da Web"). */
            String googleClientId,
            /* Domínios de e-mail aceitos (ex.: aluno.uepb.edu.br). Vazio = qualquer conta Google. */
            @DefaultValue List<String> dominiosPermitidos,
            /* E-mails que viram ADMIN automaticamente ao entrar. */
            @DefaultValue List<String> adminEmails
    ) {
    }

    /** Sessão em cookie HttpOnly: o JavaScript da página nunca enxerga o token. */
    public record Sessao(
            String segredo,
            @DefaultValue("8h") Duration validade,
            @DefaultValue("monitoria_sessao") String nomeCookie,
            /* true em produção (HTTPS). Só desligue para testar em http:// fora do localhost. */
            @DefaultValue("true") boolean cookieSeguro
    ) {
    }

    public record Cors(
            @DefaultValue("http://localhost:5173") List<String> origens
    ) {
    }

    public record Email(
            @DefaultValue("false") boolean habilitado,
            @DefaultValue("Monitoria MAP <nao-responda@monitoriamap.local>") String remetente
    ) {
    }

    /** Freios contra abuso: robôs tentando login e alunos (ou scripts) lotando a fila. */
    public record Limites(
            @DefaultValue("2") int solicitacoesAtivasPorAluno,
            @DefaultValue("20") int tentativasDeLoginPorMinuto,
            @DefaultValue("6") int novasSolicitacoesPorHora
    ) {
    }
}
