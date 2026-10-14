package br.monitoriamap.topico;

import br.monitoriamap.topico.TopicoService.TopicoDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topicos")
public class TopicoController {

    private final TopicoService servico;

    public TopicoController(TopicoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<TopicoDTO> listar(@RequestParam(defaultValue = "false") boolean incluirInativos) {
        return servico.listar(incluirInativos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TopicoDTO criar(@Valid @RequestBody CriarTopicoRequest pedido) {
        return servico.criar(pedido.nome());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TopicoDTO atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarTopicoRequest pedido) {
        return servico.atualizar(id, pedido.nome(), pedido.ativo());
    }

    public record CriarTopicoRequest(
            @NotBlank(message = "Informe o nome do tópico.")
            @Size(max = 80, message = "O nome pode ter até 80 caracteres.")
            String nome) {
    }

    public record AtualizarTopicoRequest(
            @Size(max = 80, message = "O nome pode ter até 80 caracteres.")
            String nome,
            Boolean ativo) {
    }
}
