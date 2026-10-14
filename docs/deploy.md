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
1. Em https://console.cloud.google.com/apis/credentials crie um **ID do cliente OAuth → Aplicativo da Web**.
2. Em **Origens JavaScript autorizadas**, adicione a URL da Vercel (ex.: `https://monitoria-map.vercel.app`).
3. Guarde o *Client ID*. O *Client secret* não é usado.

## 3. API: Render (exemplo)
1. **New → Web Service**, aponte para o repositório e escolha **Docker** com *Root directory* `backend`.
2. Variáveis de ambiente:

| Variável | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | dados do Neon |
| `APP_SESSAO_SEGREDO` | resultado de `openssl rand -base64 48` |
| `APP_AUTH_GOOGLE_CLIENT_ID` | o Client ID |
| `APP_AUTH_ADMIN_EMAILS` | seu e-mail |
| `APP_AUTH_DOMINIOS_PERMITIDOS` | opcional, ex.: `aluno.uepb.edu.br` |
| `APP_URL_FRONTEND` e `APP_CORS_ORIGENS` | a URL da Vercel |
| `APP_EMAIL_HABILITADO` e `SPRING_MAIL_*` | opcional (veja `.env.example`) |

3. **Não** defina `SPRING_PROFILES_ACTIVE=dev`. Sem esse perfil, o Swagger
   simplesmente não existem.
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
- [ ] A API sobe **sem** o perfil `dev` e com o e-mail ligado (`APP_EMAIL_HABILITADO=true` + `SPRING_MAIL_*`):
      sem e-mail ninguém recebe o código para ativar a conta
- [ ] `/swagger-ui.html` responde 404 em produção
- [ ] Criar a sua conta com o e-mail admin e confirmar o código que chega no e-mail
- [ ] No DevTools → Application → Cookies: `monitoria_sessao` aparece como **HttpOnly** e **Secure**
- [ ] No DevTools → Application → Local Storage: nenhum token
- [ ] Promover os monitores em **Gestão → Pessoas**
- [ ] Abrir um pedido de teste e conferir que a fila, o som e o e-mail funcionam
