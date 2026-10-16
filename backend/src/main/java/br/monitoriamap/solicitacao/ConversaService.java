package br.monitoriamap.solicitacao;

import br.monitoriamap.common.AcessoNegadoException;
import br.monitoriamap.common.RecursoNaoEncontradoException;
import br.monitoriamap.solicitacao.dto.MensagemDTO;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Conversa do pedido (Facade, como o {@link SolicitacaoService}): carrega, pergunta ao
 * domínio se pode, grava e avisa. Quem pode escrever é decidido pelo estado da solicitação.
 */
@Service
@Transactional
public class ConversaService {

    private final SolicitacaoRepository solicitacoes;
    private final MensagemRepository mensagens;
    private final UsuarioRepository usuarios;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    public ConversaService(SolicitacaoRepository solicitacoes, MensagemRepository mensagens,
                           UsuarioRepository usuarios, ApplicationEventPublisher eventos, Clock relogio) {
        this.solicitacoes = solicitacoes;
        this.mensagens = mensagens;
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<MensagemDTO> listar(Long solicitacaoId, Long usuarioId) {
        Solicitacao solicitacao = carregarParaLeitura(solicitacaoId, buscarUsuario(usuarioId));
        return mensagens.findBySolicitacaoIdOrderByEnviadaEmAscIdAsc(solicitacao.getId()).stream()
                .map(MensagemDTO::de)
                .toList();
    }

    public MensagemDTO enviar(Long solicitacaoId, Long usuarioId, String texto) {
        Usuario autor = buscarUsuario(usuarioId);
        Solicitacao solicitacao = carregarParaLeitura(solicitacaoId, autor);

        MensagemSolicitacao mensagem = mensagens.save(solicitacao.enviarMensagem(autor, texto, relogio.instant()));

        MensagemDTO dto = MensagemDTO.de(mensagem);
        eventos.publishEvent(new MensagemEnviadaEvent(dto, solicitacao.getAluno().getId(),
                solicitacao.getMonitor().getId()));
        return dto;
    }

    /**
     * Aluno que não é o dono recebe 404, como no detalhe: não confirmamos nem que o pedido existe.
     * Monitor que não participa recebe 403: ele vê o pedido na fila, mas a conversa não é dele.
     */
    private Solicitacao carregarParaLeitura(Long solicitacaoId, Usuario usuario) {
        Solicitacao solicitacao = solicitacoes.findById(solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação", solicitacaoId));
        if (!solicitacao.podeLerConversa(usuario)) {
            if (!usuario.podeAtender()) {
                throw new RecursoNaoEncontradoException("Solicitação", solicitacaoId);
            }
            throw new AcessoNegadoException("A conversa é só entre o aluno e o monitor responsável.");
        }
        return solicitacao;
    }

    private Usuario buscarUsuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }
}
