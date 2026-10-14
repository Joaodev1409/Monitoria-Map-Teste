package br.monitoriamap.usuario;

import br.monitoriamap.auth.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UsuarioController {

    private final UsuarioService servico;

    public UsuarioController(UsuarioService servico) {
        this.servico = servico;
    }

    @GetMapping("/me")
    public UsuarioDTO eu(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.buscar(usuario.id());
    }

    @PatchMapping("/me")
    public UsuarioDTO atualizarMeuPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @Valid @RequestBody AtualizarPerfilRequest pedido) {
        return servico.atualizarTelefone(usuario.id(), pedido.telefone());
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioDTO> listar() {
        return servico.listar();
    }

    @PatchMapping("/usuarios/{id}/papel")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioDTO alterarPapel(@AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long id,
                                   @Valid @RequestBody AlterarPapelRequest pedido) {
        return servico.alterarPapel(id, pedido.papel(), admin.id());
    }

    @PatchMapping("/usuarios/{id}/ativo")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioDTO definirAtivo(@AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long id,
                                   @Valid @RequestBody DefinirAtivoRequest pedido) {
        return servico.definirAtivo(id, pedido.ativo(), admin.id());
    }

    public record AlterarPapelRequest(@NotNull Papel papel) {
    }

    public record DefinirAtivoRequest(@NotNull Boolean ativo) {
    }

    public record AtualizarPerfilRequest(
            @Pattern(regexp = "^$|^\\+?[0-9 ()-]{8,20}$", message = "Informe um telefone válido.")
            String telefone) {
    }
}
