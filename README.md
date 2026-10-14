# Monitoria MAP

Helpdesk da monitoria de **Métodos Avançados em Programação**. O aluno pede ajuda, todos os monitores
são avisados na hora, um deles assume o atendimento e, ao terminar, marca como concluído. Fica
registrado quando o aluno pediu, quem atendeu e quanto tempo levou.

```
Aluno abre ──▶ ABERTA ──(monitor assume)──▶ EM ATENDIMENTO ──(conclui)──▶ CONCLUÍDA
                 │  ◀──────(devolve à fila)────────┘
                 └──(aluno cancela)──▶ CANCELADA
```

O código também serve de material de aula: veja **[docs/padroes.md](docs/padroes.md)** (State,
Observer, Strategy, Facade...).

## Funcionalidades

- **Aluno:** pede ajuda (tópico, descrição, presencial ou online), acompanha o status ao vivo e é
  avisado quando um monitor assume.
- **Monitor:** fila em tempo real com som e notificação do navegador, assume, conclui com observação
  ou devolve à fila.
- **Admin:** relatórios (espera média, duração média, por monitor, por tópico, por semana), export CSV,
  promoção de alunos a monitor e cadastro de tópicos.
- Conta com e-mail institucional (@aluno.uepb.edu.br ou @servidor.uepb.edu.br) e senha, confirmada por um
  código enviado ao e-mail, com "Esqueci a senha"; e-mails de aviso e modo claro/escuro.
- Atendimento online: o aluno ou o monitor responsável colocam o link da chamada a qualquer momento.

## Tecnologias

| Camada | Stack |
|---|---|
| API | Java 17, Spring Boot 3.5 (Web, Data JPA, Security/OAuth2 Resource Server, Mail), Flyway |
| Banco | PostgreSQL 16 |
| Front-end | React 18 + TypeScript + Vite, TanStack Query, React Router |
| Tempo real | Server-Sent Events (SSE) |
| Testes | JUnit 5, AssertJ, Mockito, Testcontainers |

## Como rodar

### Opção 1: tudo com Docker (mais simples)

```bash
docker compose up --build
```

| O quê | Endereço |
|---|---|
| Aplicação | http://localhost:3000 |
| API + Swagger | http://localhost:8080/swagger-ui.html |
| Caixa de e-mails de teste (Mailpit) | http://localhost:8025 |

Crie uma conta com qualquer e-mail @aluno.uepb.edu.br: o código de confirmação chega no **Mailpit**
(http://localhost:8025), que guarda os e-mails localmente sem enviar nada. Para virar admin, coloque o seu
e-mail em `APP_AUTH_ADMIN_EMAILS` no `.env` antes de criar a conta; depois, em **Gestão → Pessoas**, promova
quem for monitor. Para testar aluno e monitor juntos, use duas janelas (uma anônima).

### Opção 2: desenvolvimento (recarrega ao salvar)

```bash
docker compose up -d postgres mailpit
```

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
cd frontend && npm install && npm run dev
```

Acesse http://localhost:5173. O Vite repassa `/api` para o Spring na porta 8080.

## Testes

```bash
cd backend && mvn test
```

Testes de unidade: regras de estado, relatórios e CSV. Rodam em segundos, sem banco.

```bash
cd backend && mvn verify
```

Inclui o teste de integração com PostgreSQL real via Testcontainers (precisa do Docker). Ele cobre o fluxo
completo pela API, o caso de **10 monitores assumindo ao mesmo tempo** (só um consegue) e as garantias
de segurança listadas abaixo.

## Segurança

| O quê | Como |
|---|---|
| Token de sessão | Cookie `HttpOnly`, `Secure` e `SameSite=Lax`. O JavaScript não lê o token e não há nada no Local Storage |
| CSRF | Todo POST/PATCH exige o cabeçalho `X-XSRF-TOKEN` igual ao cookie `XSRF-TOKEN` |
| Sair | Invalida no servidor todos os tokens emitidos antes, mesmo se alguém tiver copiado o cookie |
| Cadastro | Só e-mail institucional; a conta só é ativada com o código de 6 números enviado ao e-mail (vale 15 min, 5 tentativas) |
| Senhas | Guardadas com BCrypt; "e-mail ou senha incorretos" é a mesma resposta nos dois casos |
| Dados pessoais | Respostas mostram só nome e foto de outras pessoas; e-mails ficam com o próprio usuário e o admin |
| Pedido de outro aluno | Responde 404 (nem confirma que existe) |
| Abuso | Limite de tentativas de login por minuto, de pedidos por hora e de pedidos ativos por aluno |
| Navegador | Content-Security-Policy, `nosniff`, `Referrer-Policy` e `Permissions-Policy` no nginx/Vercel; a API responde com CSP `default-src 'none'` |
| Swagger | Só no perfil `dev` |

## Configuração

Todas as opções estão comentadas em [.env.example](.env.example). As principais:

| Variável | Para quê |
|---|---|
| `APP_SESSAO_SEGREDO` | Assina o cookie de sessão (mínimo de 32 caracteres) |
| `APP_AUTH_GOOGLE_CLIENT_ID` | Liga o botão "Continuar com o Google" |
| `APP_AUTH_DOMINIOS_PERMITIDOS` | Ex.: `aluno.uepb.edu.br,servidor.uepb.edu.br` |
| `APP_AUTH_ADMIN_EMAILS` | Quem já entra como administrador |
| `SPRING_PROFILES_ACTIVE=dev` | Swagger (e códigos no log se o e-mail estiver desligado). **Só no seu computador** |
| `APP_EMAIL_HABILITADO` + `SPRING_MAIL_*` | Envio de e-mails (ex.: Gmail com senha de app) |

### Primeiro acesso em produção
1. Configure o envio de e-mail (obrigatório: é por ele que chegam os códigos) e defina `APP_AUTH_ADMIN_EMAILS`
   com o seu e-mail institucional. Crie a sua conta e confirme o código: você vira admin.
2. Peça aos monitores para entrarem uma vez. Em **Pessoas**, mude o papel deles para *Monitor*.
3. Ajuste os **Tópicos** às listas e unidades do semestre.

Veja [docs/deploy.md](docs/deploy.md) para publicar (Vercel + Render + Neon, tudo no plano gratuito).

## Estrutura

```
backend/src/main/java/br/monitoriamap/
├── solicitacao/   domínio principal: entidade, estados (State), serviço, API
├── notificacao/   Observer + Strategy: e-mail e tempo real (SSE)
├── auth/          cadastro, senha, código por e-mail, sessão em cookie, regras de segurança
├── usuario/  topico/  relatorio/
├── config/        propriedades tipadas, relógio, Swagger
└── common/        exceções e tratamento global de erros (ProblemDetail)

frontend/src/
├── api/           cliente HTTP, tipos e consultas (TanStack Query)
├── auth/          contexto de login
├── tempo-real/    conexão SSE com reconexão, som e notificações
├── componentes/   layout, itens, botões de ação, avisos
└── paginas/       telas do aluno, do monitor e do admin
```

## Próximos passos

- [ ] Notificação por WhatsApp: basta criar um novo `Notificador` (veja `docs/padroes.md`)
- [ ] Horários da monitoria (fila "fechada" fora do horário)
- [ ] Avaliação do atendimento pelo aluno
