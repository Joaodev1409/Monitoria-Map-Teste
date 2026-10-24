package br.monitoriamap.relatorio;

import br.monitoriamap.common.RegraNegocioException;
import br.monitoriamap.config.AppProperties;
import br.monitoriamap.relatorio.ResumoRelatorio.Contagem;
import br.monitoriamap.relatorio.ResumoRelatorio.DesempenhoMonitor;
import br.monitoriamap.solicitacao.Modalidade;
import br.monitoriamap.solicitacao.Solicitacao;
import br.monitoriamap.solicitacao.SolicitacaoRepository;
import br.monitoriamap.solicitacao.StatusSolicitacao;
import br.monitoriamap.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calcula as métricas em memória a partir das solicitações do período.
 * Para o volume de uma monitoria (centenas de pedidos por semestre) isso é simples e rápido,
 * e deixa a lógica fácil de testar sem banco.
 */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private static final long LIMITE_DIAS = 400;

    private final SolicitacaoRepository solicitacoes;
    private final ExportadorCsv exportador;
    private final ZoneId fuso;

    public RelatorioService(SolicitacaoRepository solicitacoes, ExportadorCsv exportador, AppProperties propriedades) {
        this.solicitacoes = solicitacoes;
        this.exportador = exportador;
        this.fuso = propriedades.fusoHorario();
    }

    public ResumoRelatorio resumo(LocalDate de, LocalDate ate) {
        return resumir(de, ate, buscar(de, ate));
    }

    /**
     * A planilha do período, já pronta. Fica aqui (e não no controller) porque gerar o CSV lê
     * dados ligados à solicitação (aluno, monitor, tópico): precisa estar dentro da transação.
     */
    public ArquivoCsv exportarCsv(LocalDate de, LocalDate ate) {
        String nome = "monitoria-map_" + de + "_a_" + ate + ".csv";
        return new ArquivoCsv(nome, exportador.exportar(buscar(de, ate), fuso));
    }

    public record ArquivoCsv(String nome, String conteudo) {
    }

    private List<Solicitacao> buscar(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new RegraNegocioException("A data final deve ser igual ou posterior à inicial.");
        }
        if (Duration.between(de.atStartOfDay(), ate.atStartOfDay()).toDays() > LIMITE_DIAS) {
            throw new RegraNegocioException("Escolha um período de no máximo " + LIMITE_DIAS + " dias.");
        }
        Instant inicio = de.atStartOfDay(fuso).toInstant();
        Instant fim = ate.plusDays(1).atStartOfDay(fuso).toInstant();
        return solicitacoes.findByCriadaEmBetweenOrderByCriadaEmAsc(inicio, fim);
    }

    ResumoRelatorio resumir(LocalDate de, LocalDate ate, List<Solicitacao> lista) {
        Map<StatusSolicitacao, Long> porStatus = new EnumMap<>(StatusSolicitacao.class);
        for (StatusSolicitacao status : StatusSolicitacao.values()) {
            porStatus.put(status, 0L);
        }
        lista.forEach(s -> porStatus.merge(s.getStatus(), 1L, Long::sum));

        Map<Modalidade, Long> porModalidade = new EnumMap<>(Modalidade.class);
        for (Modalidade modalidade : Modalidade.values()) {
            porModalidade.put(modalidade, 0L);
        }
        lista.forEach(s -> porModalidade.merge(s.getModalidade(), 1L, Long::sum));

        return new ResumoRelatorio(
                de, ate, lista.size(), porStatus,
                mediaEmMinutos(lista, Solicitacao::tempoDeEspera),
                mediaEmMinutos(lista, Solicitacao::tempoDeAtendimento),
                desempenhoPorMonitor(lista),
                contarPorTopico(lista),
                porModalidade,
                contarPorSemana(lista));
    }

    private List<DesempenhoMonitor> desempenhoPorMonitor(List<Solicitacao> lista) {
        Map<Usuario, List<Solicitacao>> concluidasPorMonitor = lista.stream()
                .filter(s -> s.getStatus() == StatusSolicitacao.CONCLUIDA && s.getMonitor() != null)
                .collect(Collectors.groupingBy(Solicitacao::getMonitor));

        return concluidasPorMonitor.entrySet().stream()
                .map(e -> new DesempenhoMonitor(e.getKey().getId(), e.getKey().getNome(), e.getValue().size(),
                        mediaEmMinutos(e.getValue(), Solicitacao::tempoDeAtendimento)))
                .sorted(Comparator.comparingLong(DesempenhoMonitor::atendimentosConcluidos).reversed()
                        .thenComparing(DesempenhoMonitor::nome))
                .toList();
    }

    private List<Contagem> contarPorTopico(List<Solicitacao> lista) {
        return lista.stream()
                .collect(Collectors.groupingBy(s -> s.getTopico().getNome(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new Contagem(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Contagem::total).reversed().thenComparing(Contagem::rotulo))
                .toList();
    }

    /** Agrupa pela segunda-feira de cada semana; o rótulo é a data dessa segunda (AAAA-MM-DD). */
    private List<Contagem> contarPorSemana(List<Solicitacao> lista) {
        Map<LocalDate, Long> porSemana = new TreeMap<>();
        for (Solicitacao s : lista) {
            LocalDate segunda = s.getCriadaEm().atZone(fuso).toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            porSemana.merge(segunda, 1L, Long::sum);
        }
        return porSemana.entrySet().stream()
                .map(e -> new Contagem(e.getKey().toString(), e.getValue()))
                .toList();
    }

    private static Double mediaEmMinutos(Collection<Solicitacao> lista,
                                         Function<Solicitacao, Optional<Duration>> medida) {
        List<Duration> duracoes = lista.stream().map(medida).flatMap(Optional::stream).toList();
        if (duracoes.isEmpty()) {
            return null;
        }
        double mediaSegundos = duracoes.stream().mapToLong(Duration::toSeconds).average().orElse(0);
        return Math.round(mediaSegundos / 60.0 * 10) / 10.0;
    }
}
