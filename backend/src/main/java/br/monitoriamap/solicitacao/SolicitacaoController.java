package br.monitoriamap.solicitacao;

import br.monitoriamap.auth.UsuarioAutenticado;
import br.monitoriamap.solicitacao.dto.ConcluirRequest;
import br.monitoriamap.solicitacao.dto.LinkRequest;
import br.monitoriamap.solicitacao.dto.MensagemDTO;
import br.monitoriamap.solicitacao.dto.MensagemRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import br.monitoriamap.solicitacao.dto.NovaSolicitacaoRequest;
import br.monitoriamap.solicitacao.dto.SolicitacaoDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/solicitacoes")
public class SolicitacaoController {

    private final SolicitacaoService servico;
    private final ConversaService conversa;

    public SolicitacaoController(SolicitacaoService servico, ConversaService conversa) {
        this.servico = servico;
        this.conversa = conversa;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitacaoDTO abrir(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                @Valid @RequestBody NovaSolicitacaoRequest pedido) {
        return servico.abrir(usuario.id(), pedido);
    }

    @GetMapping("/minhas")
    public List<SolicitacaoDTO> minhas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarDoAluno(usuario.id());
    }

    /** Fila dos monitores (por padrão, as abertas, da mais antiga para a mais nova). */
    @GetMapping
    @PreAuthorize("hasRole('MONITOR')")
    public List<SolicitacaoDTO> porStatus(@RequestParam(defaultValue = "ABERTA") StatusSolicitacao status) {
        return servico.listarPorStatus(status);
    }

    @GetMapping("/atendimentos")
    @PreAuthorize("hasRole('MONITOR')")
    public List<SolicitacaoDTO> meusAtendimentos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarAtendimentosDoMonitor(usuario.id());
    }

    @GetMapping("/{id}")
    public SolicitacaoDTO detalhar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return servico.detalhar(id, usuario.id());
    }

    @PostMapping("/{id}/assumir")
    @PreAuthorize("hasRole('MONITOR')")
    public SolicitacaoDTO assumir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return servico.assumir(id, usuario.id());
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasRole('MONITOR')")
    public SolicitacaoDTO concluir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                   @Valid @RequestBody(required = false) ConcluirRequest pedido) {
        return servico.concluir(id, usuario.id(), pedido == null ? null : pedido.observacao());
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasRole('MONITOR')")
    public SolicitacaoDTO devolver(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return servico.devolver(id, usuario.id());
    }

    /** Aluno ou monitor responsável colocam/trocam o link do Meet depois que o pedido já existe. */
    @PatchMapping("/{id}/link")
    public SolicitacaoDTO definirLink(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                      @Valid @RequestBody LinkRequest pedido) {
        return servico.definirLink(id, usuario.id(), pedido.linkReuniao());
    }

    @PostMapping("/{id}/cancelar")
    public SolicitacaoDTO cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return servico.cancelar(id, usuario.id());
    }

    /** Conversa do pedido: o aluno, o monitor responsável e o admin leem. */
    @GetMapping("/{id}/mensagens")
    public List<MensagemDTO> mensagens(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return conversa.listar(id, usuario.id());
    }

    /** Só o aluno e o monitor responsável escrevem, e só durante o atendimento (a regra está no State). */
    @PostMapping("/{id}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemDTO enviarMensagem(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                      @Valid @RequestBody MensagemRequest pedido) {
        return conversa.enviar(id, usuario.id(), pedido.texto());
    }
}
