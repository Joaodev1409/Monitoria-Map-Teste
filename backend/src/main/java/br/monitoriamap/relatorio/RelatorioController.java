package br.monitoriamap.relatorio;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios")
@PreAuthorize("hasRole('ADMIN')")
public class RelatorioController {

    private final RelatorioService relatorios;

    public RelatorioController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping("/resumo")
    public ResumoRelatorio resumo(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return relatorios.resumo(de, ate);
    }

    /** O controller só cuida do HTTP (cabeçalhos do download); o conteúdo vem pronto do serviço. */
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        RelatorioService.ArquivoCsv csv = relatorios.exportarCsv(de, ate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + csv.nome() + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.conteudo().getBytes(StandardCharsets.UTF_8));
    }
}
