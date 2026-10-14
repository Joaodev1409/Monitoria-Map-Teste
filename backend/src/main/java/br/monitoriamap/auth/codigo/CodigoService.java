package br.monitoriamap.auth.codigo;

import br.monitoriamap.auth.codigo.CodigoVerificacao.Finalidade;
import br.monitoriamap.usuario.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Gera, envia e confere os códigos de 6 dígitos.
 *
 * <ul>
 *   <li>Vale por 15 minutos e aceita até 5 tentativas erradas.</li>
 *   <li>Um código novo anula os anteriores da mesma finalidade.</li>
 *   <li>Só sai um código por minuto para a mesma pessoa (evita encher a caixa de e-mail).</li>
 * </ul>
 */
@Service
@Transactional(noRollbackFor = CodigoInvalidoException.class)
public class CodigoService {

    private static final Duration VALIDADE = Duration.ofMinutes(15);
    private static final Duration ESPERA_ENTRE_ENVIOS = Duration.ofSeconds(60);

    private final SecureRandom aleatorio = new SecureRandom();
    private final CodigoVerificacaoRepository codigos;
    private final EnvioDeCodigo envio;
    private final PasswordEncoder hash;
    private final Clock relogio;

    public CodigoService(CodigoVerificacaoRepository codigos, EnvioDeCodigo envio, PasswordEncoder hash, Clock relogio) {
        this.codigos = codigos;
        this.envio = envio;
        this.hash = hash;
        this.relogio = relogio;
    }

    /** Envia um código novo, a menos que um tenha saído há menos de um minuto. */
    public void enviar(Usuario usuario, Finalidade finalidade) {
        Instant agora = relogio.instant();
        boolean recente = codigos.findFirstByUsuarioIdAndFinalidadeOrderByCriadoEmDesc(usuario.getId(), finalidade)
                .filter(c -> c.podeSerUsado(agora))
                .filter(c -> c.getCriadoEm().isAfter(agora.minus(ESPERA_ENTRE_ENVIOS)))
                .isPresent();
        if (recente) {
            return;
        }

        codigos.findByUsuarioIdAndFinalidadeAndUsadoFalse(usuario.getId(), finalidade)
                .forEach(CodigoVerificacao::invalidar);
        String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
        codigos.save(new CodigoVerificacao(usuario, finalidade, hash.encode(codigo), agora, agora.plus(VALIDADE)));
        envio.enviar(usuario, finalidade, codigo);
    }

    /** Confere o código; se estiver errado, conta a tentativa (e a contagem fica gravada). */
    public void validar(Usuario usuario, Finalidade finalidade, String codigoDigitado) {
        Instant agora = relogio.instant();
        CodigoVerificacao codigo = codigos
                .findFirstByUsuarioIdAndFinalidadeOrderByCriadoEmDesc(usuario.getId(), finalidade)
                .filter(c -> c.podeSerUsado(agora))
                .orElseThrow(() -> new CodigoInvalidoException("Este código expirou. Peça um novo."));

        String limpo = codigoDigitado == null ? "" : codigoDigitado.replaceAll("\\D", "");
        if (!hash.matches(limpo, codigo.getCodigoHash())) {
            codigo.registrarTentativaErrada();
            int restantes = codigo.tentativasRestantes();
            throw new CodigoInvalidoException(restantes == 0
                    ? "Muitas tentativas erradas. Peça um novo código."
                    : "Código incorreto. Você ainda tem " + restantes + (restantes == 1 ? " tentativa." : " tentativas."));
        }
        codigo.marcarUsado();
    }
}
