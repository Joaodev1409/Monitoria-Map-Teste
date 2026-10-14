package br.monitoriamap.auth;

import br.monitoriamap.config.AppProperties;
import br.monitoriamap.usuario.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

/**
 * Emite e valida o JWT da sessão (HS256). O token vai num cookie HttpOnly e carrega só o id
 * do usuário: nome, e-mail e papel ficam no banco, que é consultado a cada requisição.
 */
@Service
public class TokenService {

    static final String EMISSOR = "monitoria-map";
    static final String CLAIM_EMITIDO_EM_MS = "emitido_em_ms";
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final AppProperties.Sessao config;
    private final Clock relogio;

    public TokenService(AppProperties propriedades, Clock relogio) {
        this.config = propriedades.sessao();
        this.relogio = relogio;
        SecretKey chave = criarChave(config.segredo());
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        this.decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = relogio.instant();
        Instant expiraEm = agora.plus(config.validade());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                // "iat" tem precisão de segundos; este campo permite invalidar com precisão ao sair.
                .claim(CLAIM_EMITIDO_EM_MS, agora.toEpochMilli())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }

    public JwtDecoder decoder() {
        return decoder;
    }

    /** Instante exato (em milissegundos) em que o token foi emitido. */
    static Instant emitidoEm(org.springframework.security.oauth2.jwt.Jwt jwt) {
        Object ms = jwt.getClaim(CLAIM_EMITIDO_EM_MS);
        return ms instanceof Number n ? Instant.ofEpochMilli(n.longValue()) : jwt.getIssuedAt();
    }

    private static SecretKey criarChave(String segredo) {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "Defina APP_SESSAO_SEGREDO com pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }
}
