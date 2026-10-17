# Publicando o Monitoria MAP

Tudo em plano gratuito, sem cartão. Leva uns 30–40 minutos na primeira vez.

```
Navegador ──▶ Vercel (site + repasse de /api) ──▶ Render (API, Docker) ──▶ Neon (PostgreSQL)
```

A Vercel não roda Java, por isso a API fica no Render. Para o navegador, porém, tudo parece um site só:
a Vercel repassa `/api/*` para o Render. Isso é necessário porque a sessão é um cookie que só vale na
mesma origem (é o que impede outros sites de agir em nome do aluno).

Siga na ordem: cada passo usa um endereço do anterior.

---

## 1. GitHub

Render e Vercel publicam a partir do repositório.

1. Em https://github.com/new crie um repositório (pode ser **privado**) chamado `monitoria-map`, **vazio**
   (sem README, sem .gitignore).
2. Na pasta do projeto:

```bash
git remote add origin https://github.com/SEU-USUARIO/monitoria-map.git
git push -u origin main
```

> O `.env` (com o Client ID e seu e-mail) não vai para o GitHub: está no `.gitignore`.

## 2. Banco: Neon

1. Em https://neon.tech entre com o GitHub e crie um projeto (`monitoria-map`) na região
   **AWS US East 1 (N. Virginia)**, a mesma da API no Render (que não tem servidor no Brasil).
   O que mais pesa é a distância API ↔ banco: cada clique faz várias consultas. Deixe o **Neon Auth desligado**
   (o login é o Google da UEPB, feito pelo próprio sistema).
2. Em **Connect**, escolha a visão de parâmetros e anote **host**, **database**, **user** e **password**.
3. Monte a URL JDBC (as tabelas são criadas sozinhas pela API na primeira subida):

```
jdbc:postgresql://HOST/DATABASE?sslmode=require
```

## 3. API: Render

1. Em https://render.com entre com o GitHub.
2. **New → Blueprint**, escolha o repositório `monitoria-map`. O Render lê o `render.yaml` e pergunta:

| Variável | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | a URL JDBC do passo 2 |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | user e password do Neon |
| `APP_AUTH_GOOGLE_CLIENT_ID` | o mesmo Client ID do `.env` |
| `APP_AUTH_ADMIN_EMAILS` | seu e-mail @aluno.uepb.edu.br |
| `APP_URL_FRONTEND` e `APP_CORS_ORIGENS` | por enquanto `https://monitoria-map.vercel.app` (ajuste no passo 5 se a Vercel der outro nome) |
| `APP_CONTATO_EMAIL` | e-mail para pedidos de exclusão de dados (aparece na política de privacidade) |

   `APP_SESSAO_SEGREDO` o Render gera sozinho.
3. Clique em **Apply**. O primeiro build leva uns 5–10 minutos.
4. Anote o endereço da API (ex.: `https://monitoria-map-api.onrender.com`) e confira
   `https://monitoria-map-api.onrender.com/actuator/health` → `{"status":"UP"}`.

> No plano gratuito a API "dorme" após ~15 min sem uso; o primeiro acesso depois disso espera ela subir
> de novo, e o site mostra uma tela de abertura enquanto isso. Para os horários de monitoria, crie um monitor
> gratuito no https://uptimerobot.com chamando o `/actuator/health` a cada 10 minutos.

### Por que o Dockerfile "treina" a aplicação no build

A máquina gratuita do Render tem só 0,1 de CPU. Ao subir, a JVM passa a maior parte do tempo lendo e
verificando milhares de classes (Spring, Hibernate, Tomcat…). Por isso o `backend/Dockerfile`:

1. **Faz uma subida de treino durante o build** (`-Dspring.context.exit=onRefresh`). O app sobe até o
   contexto do Spring ficar pronto e encerra, e a JVM grava as classes já processadas em
   `application.jsa` (CDS, *Class Data Sharing*). Na subida de verdade ela só mapeia esse arquivo.
   Como não há banco no build, o treino desliga o Flyway e o acesso do Hibernate ao banco.
2. **Usa flags pensadas para pouca CPU**: só o compilador rápido (`-XX:TieredStopAtLevel=1`) e o coletor
   de lixo mais simples (`-XX:+UseSerialGC`).

Medição numa simulação da máquina do Render (`docker run --cpus=0.1 --memory=512m`, banco já migrado),
do container criado até o `/actuator/health` responder:

| Imagem | Rodada 1 | Rodada 2 |
|---|---|---|
| Antes (jar comum) | 268 s | 198 s |
| Depois (CDS + flags) | 59 s | 47 s |

## 4. Site: Vercel

1. No arquivo `frontend/vercel.json`, troque `https://SUA-API.onrender.com` pelo endereço da API do
   passo 3, faça commit e push:

```bash
git commit -am "Aponta a Vercel para a API do Render"
```

```bash
git push
```

2. Em https://vercel.com entre com o GitHub → **Add New → Project** → escolha o repositório.
3. Em **Root Directory** escolha `frontend` (o Vite é detectado sozinho). Nenhuma variável de ambiente.
4. **Deploy**. Anote o endereço (ex.: `https://monitoria-map.vercel.app`).

## 5. Ajustes finais

1. **Render:** se o endereço da Vercel for diferente do que você pôs no passo 3, corrija
   `APP_URL_FRONTEND` e `APP_CORS_ORIGENS` em *Environment* (o Render reinicia sozinho).
2. **Google Cloud** (https://console.cloud.google.com/auth/clients?project=monitoria-map):
   - No cliente "Monitoria MAP", em **Origens JavaScript autorizadas**, adicione o endereço da Vercel.
   - Em **Branding**: *Página inicial do aplicativo* = endereço da Vercel; *Link da Política de
     Privacidade* = endereço da Vercel + `/privacidade`; em *Domínios autorizados* adicione o domínio
     da Vercel (ex.: `monitoria-map.vercel.app`). Salve.
   - Em **Público-alvo**, clique em **Publicar app**. Como o sistema só pede nome, e-mail e foto,
     o Google não exige revisão. Depois disso qualquer conta @aluno.uepb.edu.br entra, sem lista de
     usuários de teste.

## Checklist

- [ ] `https://SEU-SITE.vercel.app` abre a tela de login com o botão do Google
- [ ] Entrar com a sua conta da UEPB → você aparece como admin (menu **Gestão**)
- [ ] Tentar com um Gmail pessoal → deve recusar
- [ ] `https://SEU-SITE.vercel.app/privacidade` abre a política
- [ ] DevTools → Application → Cookies: `monitoria_sessao` com **HttpOnly** e **Secure**; Local Storage sem token
- [ ] Promover os monitores em **Gestão → Pessoas** (depois que eles entrarem uma vez)
- [ ] Abrir um pedido de teste e ver chegar na fila de outro navegador

## Opcional: e-mails de aviso

Sem e-mail o sistema funciona; os monitores são avisados na tela (com som) quando estão com ele aberto.
Para também mandar e-mail (pedido novo, assumido, link da reunião), adicione no Render as variáveis
`APP_EMAIL_HABILITADO=true`, `APP_EMAIL_REMETENTE` e `SPRING_MAIL_*` (modelos no `.env.example`), usando
um Gmail com senha de app ou um serviço como o Brevo.
