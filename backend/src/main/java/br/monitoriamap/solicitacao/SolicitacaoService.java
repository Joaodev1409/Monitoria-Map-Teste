package br.monitoriamap.solicitacao;

import br.monitoriamap.common.RecursoNaoEncontradoException;
import br.monitoriamap.common.RegraNegocioException;
import br.monitoriamap.config.AppProperties;
import br.monitoriamap.solicitacao.dto.NovaSolicitacaoRequest;
import br.monitoriamap.solicitacao.dto.SolicitacaoDTO;
import br.monitoriamap.topico.Topico;
import br.monitoriamap.topico.TopicoRepository;
import br.monitoriamap.usuario.Usuario;
import br.monitoriamap.usuario.UsuarioRepository;
import br.monitoriamap.usuario.UsuarioResumoDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Camada de serviço (Facade): o controller chama um método simples, e aqui coordenamos
 * repositórios, regras do domínio e a publicação de eventos.
 *
 * <p>Repare que o serviço não decide se uma transição é válida: isso é responsabilidade
 * da própria {@link Solicitacao} e dos seus estados.
 */
@Service
@Transactional
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacoes;
    private final UsuarioRepository usuarios;
    private final TopicoRepository topicos;
    private final ApplicationEventPublisher eventos;
    private final AppProperties.Limites limites;
    private final Clock relogio;

    public SolicitacaoService(SolicitacaoRepository solicitacoes, UsuarioRepository usuarios,
                              TopicoRepository topicos, ApplicationEventPublisher eventos,
                              AppProperties propriedades, Clock relogio) {
        this.solicitacoes = solicitacoes;
        this.usuarios = usuarios;
        this.topicos = topicos;
        this.eventos = eventos;
        this.limites = propriedades.limites();
        this.relogio = relogio;
    }

    public SolicitacaoDTO abrir(Long alunoId, NovaSolicitacaoRequest pedido) {
        Usuario aluno = usuarios.buscar(alunoId);
        long ativas = solicitacoes.countByAlunoIdAndStatusIn(alunoId, StatusSolicitacao.ATIVOS);
        if (ativas >= limites.solicitacoesAtivasPorAluno()) {
            throw new RegraNegocioException("Você já tem " + ativas + " solicitações em andamento. "
                    + "Espere uma ser concluída (ou cancele uma) antes de abrir outra.");
        }
        Topico topico = topicos.findById(pedido.topicoId())
                .filter(Topico::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Escolha um tópico válido."));

        Solicitacao solicitacao = Solicitacao.abrir(aluno, topico, pedido.titulo(), pedido.descricao(),
                pedido.modalidade(), pedido.linkReuniao(), agora());
        solicitacoes.save(solicitacao);

        return publicar(TipoEvento.CRIADA, solicitacao, aluno);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoDTO> listarDoAluno(Long alunoId) {
        return solicitacoes.findByAlunoIdOrderByCriadaEmDesc(alunoId).stream()
                .map(SolicitacaoDTO::resumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoDTO> listarPorStatus(StatusSolicitacao status) {
        return solicitacoes.findByStatusOrderByCriadaEmAsc(status).stream()
                .map(SolicitacaoDTO::resumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoDTO> listarAtendimentosDoMonitor(Long monitorId) {
        return solicitacoes.findByMonitorIdOrderByAssumidaEmDesc(monitorId).stream()
                .map(SolicitacaoDTO::resumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public SolicitacaoDTO detalhar(Long id, Long usuarioId) {
        Solicitacao solicitacao = solicitacoes.buscarDetalhada(id);
        // 404 (e não 403) para o pedido de outro aluno: não confirmamos nem que ele existe.
        if (!solicitacao.podeSerVistaPor(usuarios.buscar(usuarioId))) {
            throw new RecursoNaoEncontradoException("Solicitação", id);
        }
        return SolicitacaoDTO.detalhe(solicitacao);
    }

    public SolicitacaoDTO assumir(Long id, Long monitorId) {
        return executar(id, monitorId, TipoEvento.ASSUMIDA, (s, monitor) -> s.assumir(monitor, agora()));
    }

    public SolicitacaoDTO concluir(Long id, Long monitorId, String observacao) {
        return executar(id, monitorId, TipoEvento.CONCLUIDA, (s, monitor) -> s.concluir(monitor, observacao, agora()));
    }

    public SolicitacaoDTO devolver(Long id, Long monitorId) {
        return executar(id, monitorId, TipoEvento.DEVOLVIDA, (s, monitor) -> s.devolver(monitor, agora()));
    }

    public SolicitacaoDTO definirLink(Long id, Long usuarioId, String link) {
        return executar(id, usuarioId, TipoEvento.LINK_DEFINIDO, (s, usuario) -> s.definirLink(usuario, link, agora()));
    }

    public SolicitacaoDTO cancelar(Long id, Long usuarioId) {
        return executar(id, usuarioId, TipoEvento.CANCELADA, (s, usuario) -> s.cancelar(usuario, agora()));
    }

    /**
     * Fluxo comum de toda mudança de estado: carregar, aplicar a ação, gravar e avisar.
     * O {@code saveAndFlush} força o UPDATE agora, então um conflito de versão
     * (dois monitores assumindo juntos) aparece aqui mesmo e vira 409.
     */
    private SolicitacaoDTO executar(Long id, Long usuarioId, TipoEvento tipo, BiConsumer<Solicitacao, Usuario> acao) {
        Solicitacao solicitacao = solicitacoes.buscarDetalhada(id);
        Usuario usuario = usuarios.buscar(usuarioId);

        acao.accept(solicitacao, usuario);
        solicitacoes.saveAndFlush(solicitacao);

        return publicar(tipo, solicitacao, usuario);
    }

    private SolicitacaoDTO publicar(TipoEvento tipo, Solicitacao solicitacao, Usuario autor) {
        SolicitacaoDTO dto = SolicitacaoDTO.detalhe(solicitacao);
        eventos.publishEvent(new SolicitacaoAlteradaEvent(tipo, dto, UsuarioResumoDTO.de(autor)));
        return dto;
    }

    private Instant agora() {
        return relogio.instant();
    }
}
