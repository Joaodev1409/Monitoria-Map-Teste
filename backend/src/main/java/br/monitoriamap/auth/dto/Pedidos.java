package br.monitoriamap.auth.dto;

import br.monitoriamap.auth.codigo.CodigoVerificacao.Finalidade;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Os formulários de conta: cadastro, entrada, código e troca de senha. */
public final class Pedidos {

    private Pedidos() {
    }

    private static final String EMAIL_INVALIDO = "Informe um e-mail válido.";
    private static final String SENHA_CURTA = "A senha precisa ter de 8 a 72 caracteres.";
    private static final String CODIGO_INVALIDO = "O código tem 6 números.";

    public record Cadastro(
            @NotBlank(message = "Informe o seu nome.") @Size(max = 120, message = "O nome pode ter até 120 caracteres.")
            String nome,
            @NotBlank(message = EMAIL_INVALIDO) @Email(message = EMAIL_INVALIDO) @Size(max = 200)
            String email,
            @NotBlank(message = SENHA_CURTA) @Size(min = 8, max = 72, message = SENHA_CURTA)
            String senha) {
    }

    public record Entrar(
            @NotBlank(message = EMAIL_INVALIDO) @Email(message = EMAIL_INVALIDO) String email,
            @NotBlank(message = "Informe a sua senha.") @Size(max = 72) String senha) {
    }

    public record Confirmar(
            @NotBlank(message = EMAIL_INVALIDO) @Email(message = EMAIL_INVALIDO) String email,
            @NotBlank(message = CODIGO_INVALIDO) @Pattern(regexp = "^\\s*\\d{3}\\s*\\d{3}\\s*$", message = CODIGO_INVALIDO)
            String codigo) {
    }

    public record SoEmail(
            @NotBlank(message = EMAIL_INVALIDO) @Email(message = EMAIL_INVALIDO) String email) {
    }

    public record Reenviar(
            @NotBlank(message = EMAIL_INVALIDO) @Email(message = EMAIL_INVALIDO) String email,
            @NotNull Finalidade finalidade) {
    }

    public record RedefinirSenha(
            @NotBlank(message = EMAIL_INVALIDO) @Email(message = EMAIL_INVALIDO) String email,
            @NotBlank(message = CODIGO_INVALIDO) @Pattern(regexp = "^\\s*\\d{3}\\s*\\d{3}\\s*$", message = CODIGO_INVALIDO)
            String codigo,
            @NotBlank(message = SENHA_CURTA) @Size(min = 8, max = 72, message = SENHA_CURTA)
            String novaSenha) {
    }

    /** Resposta neutra: não confirma se o e-mail tem conta (evita descobrir quem está cadastrado). */
    public record Aviso(String mensagem) {
    }
}
