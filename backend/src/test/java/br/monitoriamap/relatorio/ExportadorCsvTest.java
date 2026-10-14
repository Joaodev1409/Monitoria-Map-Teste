package br.monitoriamap.relatorio;

import br.monitoriamap.solicitacao.Solicitacao;
import br.monitoriamap.usuario.Papel;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;

import static br.monitoriamap.Fixtures.INICIO;
import static br.monitoriamap.Fixtures.solicitacao;
import static br.monitoriamap.Fixtures.topico;
import static br.monitoriamap.Fixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;

class ExportadorCsvTest {

    @Test
    void escapaSeparadorAspasEQuebrasDeLinha() {
        assertThat(ExportadorCsv.escapar("simples")).isEqualTo("simples");
        assertThat(ExportadorCsv.escapar("a;b")).isEqualTo("\"a;b\"");
        assertThat(ExportadorCsv.escapar("diz \"oi\"")).isEqualTo("\"diz \"\"oi\"\"\"");
        assertThat(ExportadorCsv.escapar("linha1\nlinha2")).isEqualTo("\"linha1\nlinha2\"");
        assertThat(ExportadorCsv.escapar(null)).isEmpty();
    }

    @Test
    void geraCabecalhoEUmaLinhaPorSolicitacaoNoFusoLocal() {
        Solicitacao s = solicitacao(usuario(1, "Maria", Papel.ALUNO), topico(1, "Observer"), INICIO);
        String csv = new ExportadorCsv().exportar(List.of(s), ZoneId.of("America/Fortaleza"));

        String[] linhas = csv.split("\r\n");
        assertThat(linhas[0]).startsWith("﻿id;criada_em;status");
        // 13:00 UTC = 10:00 em João Pessoa/Campina Grande
        assertThat(linhas[1]).contains("14/10/2026 10:00").contains(";aberta;Observer;online;Maria;");
    }
}
