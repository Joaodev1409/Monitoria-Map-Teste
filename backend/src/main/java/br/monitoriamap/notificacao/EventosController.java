package br.monitoriamap.notificacao;

import br.monitoriamap.auth.UsuarioAutenticado;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/eventos")
public class EventosController {

    private final PainelTempoRealNotificador painel;

    public EventosController(PainelTempoRealNotificador painel) {
        this.painel = painel;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return painel.conectar(usuario);
    }
}
