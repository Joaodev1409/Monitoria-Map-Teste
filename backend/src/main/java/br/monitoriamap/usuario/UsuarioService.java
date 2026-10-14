package br.monitoriamap.usuario;

import br.monitoriamap.common.RecursoNaoEncontradoException;
import br.monitoriamap.common.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscar(Long id) {
        return UsuarioDTO.de(carregar(id));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar() {
        return usuarios.findAllByOrderByNomeAsc().stream().map(UsuarioDTO::de).toList();
    }

    public UsuarioDTO alterarPapel(Long id, Papel papel, Long quemAlteraId) {
        if (id.equals(quemAlteraId) && papel != Papel.ADMIN) {
            throw new RegraNegocioException("Você não pode remover o seu próprio acesso de administrador.");
        }
        Usuario usuario = carregar(id);
        usuario.alterarPapel(papel);
        return UsuarioDTO.de(usuario);
    }

    public UsuarioDTO definirAtivo(Long id, boolean ativo, Long quemAlteraId) {
        if (id.equals(quemAlteraId) && !ativo) {
            throw new RegraNegocioException("Você não pode desativar a sua própria conta.");
        }
        Usuario usuario = carregar(id);
        usuario.definirAtivo(ativo);
        return UsuarioDTO.de(usuario);
    }

    public UsuarioDTO atualizarTelefone(Long id, String telefone) {
        Usuario usuario = carregar(id);
        usuario.definirTelefone(telefone == null || telefone.isBlank() ? null : telefone.trim());
        return UsuarioDTO.de(usuario);
    }

    private Usuario carregar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }
}
