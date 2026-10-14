package br.monitoriamap.relatorio;

import br.monitoriamap.solicitacao.Modalidade;
import br.monitoriamap.solicitacao.StatusSolicitacao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Números da monitoria em um período. Tempos em minutos; {@code null} quando não há dados.
 */
public record ResumoRelatorio(
        LocalDate de,
        LocalDate ate,
        long total,
        Map<StatusSolicitacao, Long> porStatus,
        Double tempoMedioEsperaMinutos,
        Double tempoMedioAtendimentoMinutos,
        List<DesempenhoMonitor> porMonitor,
        List<Contagem> porTopico,
        Map<Modalidade, Long> porModalidade,
        List<Contagem> porSemana
) {

    public record DesempenhoMonitor(Long monitorId, String nome, long atendimentosConcluidos,
                                    Double tempoMedioAtendimentoMinutos) {
    }

    public record Contagem(String rotulo, long total) {
    }
}
