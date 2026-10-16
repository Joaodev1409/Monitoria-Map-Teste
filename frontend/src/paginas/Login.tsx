import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { GoogleLogin, GoogleOAuthProvider } from '@react-oauth/google';
import { Eye, LoaderCircle, ShieldCheck } from 'lucide-react';
import { api } from '../api/cliente';
import type { ConfiguracaoLogin, Solicitacao } from '../api/tipos';
import { useAutenticacao } from '../auth/Autenticacao';
import { CaixaErro, SeloStatus } from '../componentes/Basicos';
import { Etapas } from '../componentes/Etapas';
import { SimboloMarca } from '../componentes/Marca';

/**
 * Entrada única: a conta Google da UEPB. Não há cadastro nem senha própria;
 * o servidor só aceita contas @aluno.uepb.edu.br e @servidor.uepb.edu.br da organização.
 */
export function Login() {
  const config = useQuery({
    queryKey: ['config-login'],
    queryFn: () => api<ConfiguracaoLogin>('/api/auth/config'),
    staleTime: Infinity,
    // No plano gratuito a API "dorme" e leva até ~1 min para acordar: insiste por um tempo.
    retry: 8,
    retryDelay: (tentativa) => Math.min(10_000, 2_000 * 2 ** tentativa),
  });
  const acordando = useDemorou(config.isPending, 4_000);
  const { entrarComGoogle } = useAutenticacao();
  const [erro, setErro] = useState<unknown>(null);
  // Entre o "ok" do Google e a resposta da nossa API passam alguns segundos (mais se a API
  // estiver acordando): sem um aviso, parece que o clique não fez nada.
  const [entrando, setEntrando] = useState(false);
  const dominios = config.data?.dominiosPermitidos ?? [];

  return (
    <main className="login">
      <section>
        <div className="marca">
          <SimboloMarca />
          Monitoria MAP
        </div>
        <h1 className="login-titulo">
          Travou no código? <span>A monitoria te ajuda.</span>
        </h1>
        <p className="login-texto">
          Peça ajuda em Métodos Avançados em Programação e acompanhe ao vivo quando um monitor assumir o seu pedido.
        </p>

        <div className="login-acesso">
          {acordando && (
            <p className="secundario" role="status" aria-live="polite" style={{ fontSize: 15 }}>
              Acordando o servidor… isso leva até um minuto quando ninguém usou o sistema há algum tempo.
            </p>
          )}

          {config.isError && (
            <>
              <CaixaErro erro={new Error('O servidor não respondeu. Espere alguns segundos e tente de novo.')} />
              <button className="botao botao-fantasma" style={{ marginTop: 12 }} onClick={() => config.refetch()}>
                Tentar de novo
              </button>
            </>
          )}

          {config.data?.googleClientId && (
            <>
              {entrando ? (
                <p className="login-entrando" role="status" aria-live="polite">
                  <LoaderCircle size={20} aria-hidden className="girando" />
                  Entrando com a sua conta da UEPB…
                </p>
              ) : (
                <GoogleOAuthProvider clientId={config.data.googleClientId}>
                  <GoogleLogin
                    onSuccess={(resposta) => {
                      if (!resposta.credential) return;
                      setErro(null);
                      setEntrando(true);
                      entrarComGoogle(resposta.credential).catch((e) => {
                        setEntrando(false);
                        setErro(e);
                      });
                    }}
                    onError={() => setErro(new Error('Não foi possível entrar com o Google. Tente de novo.'))}
                    text="signin_with"
                    shape="pill"
                    size="large"
                    width="360"
                    locale="pt-BR"
                  />
                </GoogleOAuthProvider>
              )}
              {dominios.length > 0 && (
                <p className="secundario" style={{ display: 'flex', gap: 8, marginTop: 16, fontSize: 15 }}>
                  <ShieldCheck size={18} aria-hidden style={{ flexShrink: 0, marginTop: 2, color: 'var(--acao-texto)' }} />
                  <span>
                    Use a sua conta da UEPB (
                    {dominios.map((d, i) => (
                      <span key={d}>
                        {i > 0 && ' ou '}
                        <strong>@{d}</strong>
                      </span>
                    ))}
                    ). Não precisa criar senha.
                  </span>
                </p>
              )}
            </>
          )}

          {config.data && !config.data.googleClientId && (
            <CaixaErro
              erro={new Error('O login com Google ainda não foi configurado. Avise a coordenação da monitoria.')}
            />
          )}

          {erro !== null && (
            <div style={{ marginTop: 16 }}>
              <CaixaErro erro={erro} />
            </div>
          )}
        </div>

        <p className="rodape-login">
          Projeto da monitoria de MAP, não oficial da UEPB · <Link to="/privacidade">Política de privacidade</Link>
        </p>
      </section>

      <ExemploAoVivo />
    </main>
  );
}

/**
 * Demonstração do que o aluno vê depois de pedir ajuda: o pedido avança sozinho pelas
 * etapas. Os dados são fictícios e estão identificados como exemplo.
 */
function ExemploAoVivo() {
  const [passo, setPasso] = useState(0);
  useEffect(() => {
    const id = setInterval(() => setPasso((p) => (p + 1) % 3), 2600);
    return () => clearInterval(id);
  }, []);

  const status = (['ABERTA', 'EM_ATENDIMENTO', 'CONCLUIDA'] as const)[passo];
  const exemplo: Solicitacao = {
    id: 0,
    titulo: 'Quando usar Observer em vez de Mediator?',
    descricao: '',
    topico: { id: 0, nome: 'Padrões comportamentais' },
    modalidade: 'PRESENCIAL',
    linkReuniao: null,
    status,
    aluno: { id: 0, nome: 'Você', fotoUrl: null },
    monitor: passo > 0 ? { id: 1, nome: 'Ana', fotoUrl: null } : null,
    criadaEm: new Date(Date.now() - 4 * 60_000).toISOString(),
    assumidaEm: null,
    concluidaEm: null,
    canceladaEm: null,
    observacaoConclusao: null,
    historico: [],
  };

  return (
    <section className="login-exemplo" aria-label="Exemplo de pedido acompanhado ao vivo">
      <div className="cartao cartao-corpo">
        <span className="rotulo-exemplo">
          <Eye size={16} aria-hidden />
          Exemplo de como você acompanha um pedido
        </span>
        <div className="titulo-com-selo">
          <p className="destaque-titulo" style={{ fontSize: '1.2rem' }}>
            {exemplo.titulo}
          </p>
          <SeloStatus status={status} />
        </div>
        <p className="secundario" style={{ margin: '6px 0 26px' }}>
          {exemplo.topico.nome} · Presencial
        </p>
        <Etapas solicitacao={exemplo} />
      </div>
    </section>
  );
}

/** Fica verdadeiro quando `ativo` continua verdadeiro por mais de `ms` milissegundos. */
function useDemorou(ativo: boolean, ms: number) {
  const [demorou, setDemorou] = useState(false);
  useEffect(() => {
    if (!ativo) return setDemorou(false);
    const t = setTimeout(() => setDemorou(true), ms);
    return () => clearTimeout(t);
  }, [ativo, ms]);
  return demorou;
}
