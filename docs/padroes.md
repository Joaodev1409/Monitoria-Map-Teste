# Padrões de projeto no Monitoria MAP

Este sistema também é material de estudo para a disciplina de **Métodos Avançados em Programação**.
Cada seção abaixo mostra um problema real do projeto, o padrão escolhido para resolvê-lo e onde ele
está no código.

---

## 1. State: o ciclo de vida da solicitação

### Problema
Uma solicitação passa por vários estados, e cada ação só faz sentido em alguns deles:

| Ação ↓ / Estado → | ABERTA | EM_ATENDIMENTO | CONCLUIDA | CANCELADA |
|---|---|---|---|---|
| assumir  | ✅ | ❌ "já foi assumida por Ana" | ❌ | ❌ |
| concluir | ❌ | ✅ (só o responsável) | ❌ | ❌ |
| devolver | ❌ | ✅ (só o responsável) | ❌ | ❌ |
| cancelar | ✅ (só o aluno) | ❌ | ❌ | ❌ |

A solução ingênua é encher a classe de `if (status == ...)`. Isso funciona, mas cada regra nova
espalha condições pelo código, e fica fácil esquecer um caso.

### Solução
Cada estado vira uma classe que sabe responder a cada ação. A `Solicitacao` só **delega**:

```java
public void assumir(Usuario monitor, Instant agora) {
    status.estado().assumir(this, monitor, agora);   // sem nenhum if
}
```

A interface define que, **por padrão, toda ação é inválida**. Cada estado sobrescreve só o que permite:

```java
sealed interface EstadoSolicitacao permits EstadoAberta, EstadoEmAtendimento, EstadoConcluida, EstadoCancelada {
    default void assumir(Solicitacao s, Usuario monitor, Instant agora) {
        throw TransicaoInvalidaException.de(status(), "assumir");
    }
    // concluir, devolver, cancelar... idem
}
```

```mermaid
stateDiagram-v2
    [*] --> ABERTA : aluno abre
    ABERTA --> EM_ATENDIMENTO : monitor assume
    EM_ATENDIMENTO --> ABERTA : monitor devolve
    EM_ATENDIMENTO --> CONCLUIDA : monitor conclui
    ABERTA --> CANCELADA : aluno cancela
    CONCLUIDA --> [*]
    CANCELADA --> [*]
```

```mermaid
classDiagram
    class Solicitacao {
        -StatusSolicitacao status
        +assumir(monitor, agora)
        +concluir(usuario, obs, agora)
        +devolver(usuario, agora)
        +cancelar(usuario, agora)
        ~registrarAssuncao(...)
        ~registrarConclusao(...)
    }
    class StatusSolicitacao {
        <<enum>>
        ABERTA
        EM_ATENDIMENTO
        CONCLUIDA
        CANCELADA
        ~estado() EstadoSolicitacao
    }
    class EstadoSolicitacao {
        <<interface>>
        +assumir()
        +concluir()
        +devolver()
        +cancelar()
    }
    Solicitacao --> StatusSolicitacao
    StatusSolicitacao --> EstadoSolicitacao
    EstadoSolicitacao <|.. EstadoAberta
    EstadoSolicitacao <|.. EstadoEmAtendimento
    EstadoSolicitacao <|.. EstadoConcluida
    EstadoSolicitacao <|.. EstadoCancelada
```

### Detalhes que valem discutir em aula
- **Encapsulamento.** Os métodos `registrar*` da `Solicitacao` são *package-private*. Só os estados (no
  mesmo pacote) conseguem chamá-los, então ninguém de fora muda o status pulando as regras.
- **`sealed`** (Java 17) garante que não existem outros estados além dos quatro listados.
- **Estados sem campos** podem ser compartilhados: cada constante do `enum` guarda uma única instância.
- O banco guarda só o `enum` (texto `"ABERTA"`). O objeto de estado é derivado dele.

📁 `backend/src/main/java/br/monitoriamap/solicitacao/`
`Solicitacao.java`, `StatusSolicitacao.java`, `EstadoSolicitacao.java`, `Estado*.java`
🧪 `SolicitacaoTest.java` testa cada transição, sem banco e sem Spring.

---

## 2. Observer: avisar sem acoplar

### Problema
Quando algo acontece com uma solicitação, várias partes do sistema precisam reagir: o painel em tempo
real, o e-mail e, no futuro, o WhatsApp. Se o `SolicitacaoService` chamasse cada uma diretamente, ele
dependeria de todas e teria de mudar a cada canal novo.

### Solução
O serviço apenas **publica um evento**. Quem tiver interesse **observa**:

```java
// SolicitacaoService: não sabe quem está ouvindo
eventos.publishEvent(new SolicitacaoAlteradaEvent(TipoEvento.ASSUMIDA, dto));
```

```java
// DespachanteDeNotificacoes: o observador
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void aoAlterarSolicitacao(SolicitacaoAlteradaEvent evento) { ... }
```

```mermaid
sequenceDiagram
    participant C as Controller
    participant S as SolicitacaoService
    participant P as ApplicationEventPublisher
    participant D as DespachanteDeNotificacoes
    participant E as EmailNotificador
    participant T as PainelTempoRealNotificador
    C->>S: assumir(id, monitor)
    S->>S: solicitacao.assumir(...) e salvar
    S->>P: publishEvent(SolicitacaoAlteradaEvent)
    S-->>C: 200 OK (resposta imediata)
    Note over P,D: depois do COMMIT, em outra thread
    P->>D: aoAlterarSolicitacao(evento)
    D->>E: notificar(evento)
    D->>T: notificar(evento)
```

- **`AFTER_COMMIT`**: só avisa se a transação deu certo. Ninguém recebe "assumida" de algo que foi desfeito.
- **`@Async`**: o monitor que clicou em "Assumir" não espera o e-mail ser enviado.
- O evento carrega um **DTO**, não a entidade JPA, porque os observadores rodam depois do commit e em outra
  thread, onde a sessão do Hibernate já foi fechada.

📁 `solicitacao/SolicitacaoAlteradaEvent.java`, `notificacao/DespachanteDeNotificacoes.java`

---

## 3. Strategy: um canal de notificação por classe

### Problema
Cada canal envia de um jeito: SSE para o navegador, SMTP para e-mail, uma API para WhatsApp. Mas, para
quem dispara, todos fazem a mesma coisa: *notificar*.

### Solução
Uma interface com várias implementações intercambiáveis:

```java
public interface Notificador {
    void notificar(SolicitacaoAlteradaEvent evento);
}
```

```java
@Component class EmailNotificador implements Notificador { ... }
@Component class PainelTempoRealNotificador implements Notificador { ... }
```

O Spring injeta **todas** as implementações numa lista:

```java
public DespachanteDeNotificacoes(List<Notificador> notificadores) { ... }
```

### Aberto/Fechado (o "O" do SOLID) na prática
Para adicionar o WhatsApp, basta **criar** uma classe nova. **Nenhuma linha existente muda**:

```java
@Component
public class WhatsAppNotificador implements Notificador {
    @Override
    public void notificar(SolicitacaoAlteradaEvent evento) {
        // chamar a API do WhatsApp aqui
    }
}
```

📁 `notificacao/Notificador.java`, `EmailNotificador.java`, `PainelTempoRealNotificador.java`

---

## 4. Facade: a camada de serviço

O `SolicitacaoService` oferece ao controller operações simples (`abrir`, `assumir`, `concluir`...) e
esconde a coordenação por trás: buscar entidades, validar, aplicar a regra do domínio, salvar,
publicar o evento e converter para DTO. O controller fica com poucas linhas por endpoint.

O método `executar(...)` concentra o fluxo comum a toda mudança de estado e recebe a ação específica
como um `BiConsumer` (uma pitada de **Template Method** com lambdas).

📁 `solicitacao/SolicitacaoService.java`, `solicitacao/SolicitacaoController.java`

---

## 5. Repository e DTO

- **Repository** (`SolicitacaoRepository`, `UsuarioRepository`...): o Spring Data gera a implementação a
  partir do nome do método (`findByStatusOrderByCriadaEmAsc`). Os `@EntityGraph` evitam o problema
  **N+1** ao carregar aluno, monitor e tópico junto com a lista.
- **DTO** (`SolicitacaoDTO`, records): a API nunca devolve entidades JPA. Isso evita expor campos
  internos, laços infinitos na serialização e `LazyInitializationException`.

---

## 6. Outras decisões que rendem discussão

| Decisão | Por quê | Onde |
|---|---|---|
| `@Version` (lock otimista) | Dois monitores clicam "Assumir" juntos: só um vence, o outro recebe 409 | `Solicitacao.versao`, `FluxoSolicitacaoIT` |
| Injetar `Clock` | Código que depende de "agora" fica testável | `InfraConfig`, `SolicitacaoService` |
| `ProblemDetail` (RFC 7807) | Formato padrão de erro; o front só mostra `detail` | `TratadorGlobalDeErros` |
| `@ConfigurationProperties` com records | Configuração tipada e validada em um só lugar | `AppProperties` |
| Flyway | O esquema do banco é versionado junto com o código | `resources/db/migration` |
| Papel lido do banco a cada requisição | Promover ou desativar alguém tem efeito imediato | `UsuarioAuthenticationConverter` |

### Exercícios sugeridos para a turma
1. Crie o `WhatsAppNotificador` (só um `log.info` já serve) e confirme que nada mais precisou mudar.
2. Adicione o estado **AGUARDANDO_ALUNO** (o monitor fez uma pergunta e espera a resposta). Quais
   classes mudam? Quais testes você escreveria primeiro?
3. Reescreva `Solicitacao.assumir` com `switch (status)` em vez do padrão State e compare as duas versões.
4. Hoje o `EmailNotificador` decide por `switch (evento.tipo())`. Que padrão deixaria cada tipo de e-mail
   numa classe própria? Vale a pena aqui?
