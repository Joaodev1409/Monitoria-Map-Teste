# Publicando o Monitoria MAP

A Vercel hospeda só sites estáticos e funções JavaScript; ela **não roda Spring Boot**. Por isso o
sistema é publicado em três partes, todas com plano gratuito:

```
Navegador ──▶ Vercel (React + rewrite /api) ──▶ Render / Railway / Fly.io (API, Docker) ──▶ Neon (PostgreSQL)
```

**Importante:** o navegador deve enxergar front e API na **mesma origem**. A sessão é um cookie
HttpOnly com `SameSite=Lax` e todo POST exige o token CSRF lido de um cookie; com domínios
diferentes, nada disso funciona (e não deve funcionar). Por isso a Vercel repassa `/api/*` para a
API, em vez de o React chamar o domínio do Render diretamente.

## 1. Banco: Neon
1. Crie um projeto em https://neon.tech e copie os dados de conexão.
2. Monte a URL JDBC: `jdbc:postgresql://<host>/<banco>?sslmode=require`.

As tabelas são criadas sozinhas pelo Flyway na primeira subida da API.

## 2. Login: Google

O login é só com a conta Google da UEPB. Para isso o sistema precisa de um **Client ID** (gratuito, sem cartão).
Faça com uma conta Google pessoal; se a da UEPB não deixar criar projetos, não tem problema, quem entra no
sistema continua sendo a conta da UEPB.

1. Abra https://console.cloud.google.com e crie um projeto (ex.: `monitoria-map`).
2. Menu **APIs e serviços → Tela de consentimento OAuth** (ou "Google Auth Platform → Branding"):
   - Tipo de usuário: **Externo**; nome do app: `Monitoria MAP`; e-mails de suporte e contato: o seu.
   - Escopos: não precisa adicionar nada (o padrão `email`, `profile` e `openid` basta).
   - Em **Público-alvo**, clique em **Publicar app**. Com só esses escopos, o Google não exige revisão;
     sem publicar, só os e-mails listados como "usuários de teste" conseguem entrar.
3. Menu **APIs e serviços → Credenciais → Criar credenciais → ID do cliente OAuth**:
   - Tipo: **Aplicativo da Web**.
   - **Origens JavaScript autorizadas**: `http://localhost:3000`, `http://localhost:5173` e, quando publicar,
     a URL da Vercel (ex.: `https://monitoria-map.vercel.app`).
   - Não precisa de "URIs de redirecionamento".
4. Copie o **ID do cliente** (termina em `.apps.googleusercontent.com`) para `APP_AUTH_GOOGLE_CLIENT_ID`.
   O *Client secret* não é usado.

## 3. API: Render (exemplo)
1. **New → Web Service**, aponte para o repositório e escolha **Docker** com *Root directory* `backend`.
2. Variáveis de ambiente:

| Variável | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | dados do Neon |
| `APP_SESSAO_SEGREDO` | resultado de `openssl rand -base64 48` |
| `APP_AUTH_GOOGLE_CLIENT_ID` | o Client ID |
| `APP_AUTH_ADMIN_EMAILS` | seu e-mail |
| `APP_AUTH_DOMINIOS_PERMITIDOS` | opcional; o padrão já é `aluno.uepb.edu.br,servidor.uepb.edu.br` |
| `APP_URL_FRONTEND` e `APP_CORS_ORIGENS` | a URL da Vercel |
| `APP_EMAIL_HABILITADO` e `SPRING_MAIL_*` | opcional (veja `.env.example`) |

3. **Não** defina `SPRING_PROFILES_ACTIVE=dev`. Sem esse perfil, o Swagger simplesmente não existe.
4. *Health check path*: `/actuator/health`.

> No plano gratuito do Render a API "dorme" após uns 15 min sem uso, e a primeira requisição depois disso
> demora uns 30–60 s. O front reconecta o tempo real sozinho. Para horários de monitoria, uma alternativa
> é um serviço de *ping* (ex.: UptimeRobot) chamando `/actuator/health` a cada 10 minutos.

## 4. Front-end: Vercel
1. Em `frontend/vercel.json`, troque `https://SUA-API.onrender.com` pela URL real da API.
2. **Add New → Project**, escolha o repositório e defina *Root directory* `frontend` (o Vite é detectado sozinho).
3. Não é preciso nenhuma variável de ambiente no front.

O `vercel.json` também:
- faz as rotas do React (ex.: `/monitor/fila`) funcionarem ao recarregar a página;
- envia os cabeçalhos de segurança (Content-Security-Policy, nosniff, Referrer-Policy etc.), os mesmos do `nginx.conf`.

## Checklist final
- [ ] A API sobe **sem** o perfil `dev`
- [ ] `/swagger-ui.html` responde 404 em produção
- [ ] A URL da Vercel está nas "Origens JavaScript autorizadas" do Client ID
- [ ] Entrar com a conta da UEPB do e-mail admin (você vira admin) e tentar com um Gmail pessoal (deve recusar)
- [ ] No DevTools → Application → Cookies: `monitoria_sessao` aparece como **HttpOnly** e **Secure**
- [ ] No DevTools → Application → Local Storage: nenhum token
- [ ] Promover os monitores em **Gestão → Pessoas**
- [ ] Abrir um pedido de teste e conferir que a fila e o som funcionam (e o e-mail, se configurado)
