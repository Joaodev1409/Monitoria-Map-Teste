package br.monitoriamap.relatorio;

import br.monitoriamap.solicitacao.Solicitacao;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gera um CSV pronto para abrir no Excel/Google Planilhas em português:
 * separador ";" e BOM UTF-8 (para os acentos aparecerem certos).
 */
@Component
public class ExportadorCsv {

    private static final String BOM = "﻿";
    private static final String SEPARADOR = ";";
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<String> CABECALHO = List.of(
            "id", "criada_em", "status", "topico", "modalidade", "aluno", "email_aluno", "monitor",
            "assumida_em", "concluida_em", "espera_min", "atendimento_min", "titulo", "observacao");

    public String exportar(List<Solicitacao> solicitacoes, ZoneId fuso) {
        StringBuilder csv = new StringBuilder(BOM).append(linha(CABECALHO.stream()));
        for (Solicitacao s : solicitacoes) {
            csv.append(linha(Stream.of(
                    String.valueOf(s.getId()),
                    data(s.getCriadaEm(), fuso),
                    s.getStatus().descricao(),
                    s.getTopico().getNome(),
                    s.getModalidade().name().toLowerCase(),
                    s.getAluno().getNome(),
                    s.getAluno().getEmail(),
                    s.getMonitor() == null ? "" : s.getMonitor().getNome(),
                    data(s.getAssumidaEm(), fuso),
                    data(s.getConcluidaEm(), fuso),
                    minutos(s.tempoDeEspera()),
                    minutos(s.tempoDeAtendimento()),
                    s.getTitulo(),
                    s.getObservacaoConclusao())));
        }
        return csv.toString();
    }

    private static String linha(Stream<String> campos) {
        return campos.map(ExportadorCsv::escapar).collect(Collectors.joining(SEPARADOR)) + "\r\n";
    }

    /** Campos com separador, aspas ou quebra de linha vão entre aspas, com aspas internas dobradas. */
    static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(SEPARADOR) || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private static String data(Instant instante, ZoneId fuso) {
        return instante == null ? "" : DATA_HORA.format(instante.atZone(fuso));
    }

    private static String minutos(Optional<Duration> duracao) {
        return duracao.map(d -> String.valueOf(d.toMinutes())).orElse("");
    }
}
