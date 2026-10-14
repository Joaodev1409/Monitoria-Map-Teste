package br.monitoriamap;

import br.monitoriamap.solicitacao.Modalidade;
import br.monitoriamap.solicitacao.Solicitacao;
import br.monitoriamap.topico.Topico;
import br.monitoriamap.usuario.Papel;
import br.monitoriamap.usuario.Usuario;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

/** Objetos prontos para testes de unidade (sem banco: os ids são atribuídos à mão). */
public final class Fixtures {

    public static final Instant INICIO = Instant.parse("2026-10-14T13:00:00Z");

    private Fixtures() {
    }

    public static Usuario usuario(long id, String nome, Papel papel) {
        Usuario usuario = new Usuario(nome, nome.toLowerCase().replace(' ', '.') + "@exemplo.com", papel, INICIO);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    public static Topico topico(long id, String nome) {
        Topico topico = new Topico(nome);
        ReflectionTestUtils.setField(topico, "id", id);
        return topico;
    }

    public static Solicitacao solicitacao(Usuario aluno, Topico topico, Instant criadaEm) {
        return Solicitacao.abrir(aluno, topico, "Dúvida no Observer", "Não entendi o notify.",
                Modalidade.ONLINE, "https://meet.google.com/abc-defg-hij", criadaEm);
    }
}
