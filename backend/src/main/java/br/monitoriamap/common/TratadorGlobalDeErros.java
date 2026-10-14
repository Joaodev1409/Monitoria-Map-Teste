package br.monitoriamap.common;

import br.monitoriamap.auth.EmailNaoVerificadoException;
import br.monitoriamap.auth.LoginInvalidoException;
import br.monitoriamap.solicitacao.TransicaoInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte exceções em respostas no formato padrão RFC 7807 (ProblemDetail), por exemplo:
 * <pre>{ "status": 409, "title": "Conflito", "detail": "Esta solicitação já foi assumida por Ana." }</pre>
 * O front-end só precisa mostrar o campo {@code detail}.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    ProblemDetail transicaoInvalida(TransicaoInvalidaException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail conflitoDeVersao(ObjectOptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Conflito",
                "Outra pessoa alterou esta solicitação agora mesmo. Atualize a página e tente de novo.");
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regraNegocio(RegraNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Não foi possível concluir", e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ProblemDetail acessoNegadoDominio(AcessoNegadoException e) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado(AccessDeniedException e) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para esta ação.");
    }

    /** O front usa o campo "codigo" para abrir a etapa de digitar o código. */
    @ExceptionHandler(EmailNaoVerificadoException.class)
    ProblemDetail emailNaoVerificado(EmailNaoVerificadoException e) {
        ProblemDetail problema = problema(HttpStatus.FORBIDDEN, "E-mail não confirmado", e.getMessage());
        problema.setProperty("codigo", "email-nao-verificado");
        return problema;
    }

    @ExceptionHandler(LoginInvalidoException.class)
    ProblemDetail loginInvalido(LoginInvalidoException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Não foi possível entrar", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado",
                "Algo deu errado do nosso lado. Tente novamente em instantes.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        String resumo = erros.values().stream().findFirst().orElse("Dados inválidos.");
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", resumo);
        problema.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(problema);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
