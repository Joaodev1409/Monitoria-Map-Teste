// Cliente HTTP mínimo.
//
// Segurança:
// - A sessão é um cookie HttpOnly que o navegador envia sozinho: o JavaScript nunca vê o token,
//   então não há nada no Local Storage para roubar.
// - Todo POST/PATCH leva o cabeçalho X-XSRF-TOKEN com o valor do cookie XSRF-TOKEN (proteção CSRF).
// - Front e API ficam sempre na mesma origem (proxy do Vite, do nginx ou rewrite da Vercel).

export class ErroApi extends Error {
  constructor(
    public readonly status: number,
    mensagem: string,
    public readonly erros: Record<string, string> = {},
    public readonly titulo = '',
  ) {
    super(mensagem);
  }
}

let aoExpirarSessao: () => void = () => {};

/** Chamado quando a API responde 401 numa rota protegida (sessão vencida ou encerrada). */
export function quandoSessaoExpirar(callback: () => void) {
  aoExpirarSessao = callback;
}

/** Confere com o servidor se a sessão ainda vale; se não valer, volta para a tela de login. */
export async function conferirSessao() {
  const r = await api<{ usuario: unknown }>('/api/auth/sessao').catch(() => null);
  if (r && r.usuario === null) aoExpirarSessao();
}

function lerCookie(nome: string): string | null {
  const par = document.cookie.split('; ').find((c) => c.startsWith(`${nome}=`));
  return par ? decodeURIComponent(par.slice(nome.length + 1)) : null;
}

function cabecalhos(metodo: string, comCorpo: boolean): Record<string, string> {
  const h: Record<string, string> = { Accept: 'application/json' };
  if (comCorpo) h['Content-Type'] = 'application/json';
  if (metodo !== 'GET' && metodo !== 'HEAD') {
    const csrf = lerCookie('XSRF-TOKEN');
    if (csrf) h['X-XSRF-TOKEN'] = csrf;
  }
  return h;
}

async function requisitar(caminho: string, opcoes: RequestInit): Promise<Response> {
  const metodo = (opcoes.method ?? 'GET').toUpperCase();
  try {
    return await fetch(caminho, {
      ...opcoes,
      credentials: 'same-origin',
      headers: { ...cabecalhos(metodo, Boolean(opcoes.body)), ...opcoes.headers },
    });
  } catch {
    throw new ErroApi(0, 'Sem conexão com o servidor. Confira sua internet e tente de novo.');
  }
}

async function erroDe(resposta: Response): Promise<ErroApi> {
  const problema = await resposta.json().catch(() => null);
  return new ErroApi(
    resposta.status,
    problema?.detail ?? 'Algo deu errado do nosso lado. Tente de novo em instantes.',
    problema?.erros ?? {},
    problema?.title ?? '',
  );
}

export async function api<T>(caminho: string, opcoes: RequestInit = {}): Promise<T> {
  let resposta = await requisitar(caminho, opcoes);

  // O cookie CSRF ainda não existia (ou expirou): pega um novo e tenta uma única vez.
  if (resposta.status === 403 && (opcoes.method ?? 'GET') !== 'GET') {
    const erro = await erroDe(resposta.clone());
    if (erro.titulo === 'Página desatualizada') {
      await requisitar('/api/auth/config', {});
      resposta = await requisitar(caminho, opcoes);
    }
  }

  if (resposta.status === 401 && !caminho.startsWith('/api/auth/') && caminho !== '/api/me') {
    aoExpirarSessao();
  }
  if (!resposta.ok) throw await erroDe(resposta);
  if (resposta.status === 204) return undefined as T;

  const tipo = resposta.headers.get('Content-Type') ?? '';
  return (tipo.includes('json') ? resposta.json() : resposta.text()) as Promise<T>;
}

export const enviar = <T>(caminho: string, metodo: string, corpo?: unknown) =>
  api<T>(caminho, { method: metodo, body: corpo === undefined ? undefined : JSON.stringify(corpo) });

/** Baixa um arquivo (ex.: CSV) usando a sessão atual e dispara o "salvar como" do navegador. */
export async function baixarArquivo(caminho: string, nomePadrao: string) {
  const resposta = await requisitar(caminho, {});
  if (!resposta.ok) throw await erroDe(resposta);
  const disposicao = resposta.headers.get('Content-Disposition') ?? '';
  const nome = /filename="([^"]+)"/.exec(disposicao)?.[1] ?? nomePadrao;
  const url = URL.createObjectURL(await resposta.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  link.click();
  URL.revokeObjectURL(url);
}
