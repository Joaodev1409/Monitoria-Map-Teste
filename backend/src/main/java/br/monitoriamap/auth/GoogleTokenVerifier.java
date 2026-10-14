package br.monitoriamap.auth;

import br.monitoriamap.config.AppProperties;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Valida o "ID token" que o botão "Entrar com Google" entrega ao front-end:
 * assinatura (chaves públicas do Google), validade, emissor e destinatário (o nosso Client ID).
 */
@Component
public class GoogleTokenVerifier {

    private static final String CHAVES_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISSORES = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private volatile NimbusJwtDecoder decoder;

    public GoogleTokenVerifier(AppProperties propriedades) {
        this.clientId = propriedades.auth().googleClientId();
    }

    public IdentidadeGoogle verificar(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new LoginInvalidoException("O login com Google não está configurado (APP_AUTH_GOOGLE_CLIENT_ID).");
        }
        try {
            Jwt jwt = decoder().decode(idToken);
            return new IdentidadeGoogle(
                    jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                    jwt.getClaimAsString("name"),
                    jwt.getClaimAsString("picture"));
        } catch (JwtException e) {
            throw new LoginInvalidoException("Não foi possível validar o login com Google. Tente novamente.");
        }
    }

    private NimbusJwtDecoder decoder() {
        if (decoder == null) {
            synchronized (this) {
                if (decoder == null) {
                    NimbusJwtDecoder novo = NimbusJwtDecoder.withJwkSetUri(CHAVES_GOOGLE).build();
                    novo.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                            new JwtTimestampValidator(),
                            validar(jwt -> jwt.getIssuer() != null && EMISSORES.contains(jwt.getIssuer().toString()),
                                    "Emissor inválido"),
                            validar(jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId),
                                    "Token emitido para outro aplicativo")));
                    decoder = novo;
                }
            }
        }
        return decoder;
    }

    private static OAuth2TokenValidator<Jwt> validar(java.util.function.Predicate<Jwt> regra, String erro) {
        return jwt -> regra.test(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", erro, null));
    }

    public record IdentidadeGoogle(String email, boolean emailVerificado, String nome, String fotoUrl) {
    }
}
