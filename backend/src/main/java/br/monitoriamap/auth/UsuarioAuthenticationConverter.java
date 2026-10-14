package br.monitoriamap.auth;

import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Transforma um JWT válido no usuário autenticado.
 *
 * <p>O papel é lido do banco a cada requisição (e não do token). Assim, quando o admin
 * promove alguém a monitor ou desativa uma conta, o efeito é imediato, sem novo login.
 * Pelo mesmo motivo, um token emitido antes de a pessoa clicar em "Sair" é recusado.
 */
@Component
public class UsuarioAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarios;

    public UsuarioAuthenticationConverter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = usuarios.findById(Long.valueOf(jwt.getSubject()))
                .filter(Usuario::isAtivo)
                .filter(u -> u.sessaoAindaValida(TokenService.emitidoEm(jwt)))
                .orElseThrow(() -> new InvalidBearerTokenException("Sessão encerrada ou usuário desativado."));

        return new UsernamePasswordAuthenticationToken(UsuarioAutenticado.de(usuario), jwt,
                autoridades(usuario.getPapel()));
    }

    /** ADMIN pode tudo que MONITOR pode, e MONITOR tudo que ALUNO pode. */
    private static List<SimpleGrantedAuthority> autoridades(Papel papel) {
        return switch (papel) {
            case ADMIN -> papeis("ADMIN", "MONITOR", "ALUNO");
            case MONITOR -> papeis("MONITOR", "ALUNO");
            case ALUNO -> papeis("ALUNO");
        };
    }

    private static List<SimpleGrantedAuthority> papeis(String... nomes) {
        return Arrays.stream(nomes).map(n -> new SimpleGrantedAuthority("ROLE_" + n)).toList();
    }
}
