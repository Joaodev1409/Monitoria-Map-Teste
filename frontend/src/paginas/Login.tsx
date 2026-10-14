import { useEffect, useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { GoogleLogin, GoogleOAuthProvider } from '@react-oauth/google';
import { ArrowLeft, Eye, EyeOff, MailCheck } from 'lucide-react';
import { api, enviar, ErroApi } from '../api/cliente';
import type { ConfiguracaoLogin, LoginResposta, Solicitacao } from '../api/tipos';
import { useAutenticacao } from '../auth/Autenticacao';
import { CaixaErro, SeloStatus } from '../componentes/Basicos';
import { Etapas } from '../componentes/Etapas';
import { SimboloMarca } from '../componentes/Marca';

type Etapa = 'entrar' | 'criar' | 'confirmar' | 'esqueci' | 'redefinir';

export function Login() {
  const config = useQuery({
    queryKey: ['config-login'],
    queryFn: () => api<ConfiguracaoLogin>('/api/auth/config'),
    staleTime: Infinity,
  });
  const dominios = config.data?.dominiosPermitidos ?? [];
  const [etapa, setEtapa] = useState<Etapa>('entrar');
  const [email, setEmail] = useState('');

  const irPara = (proxima: Etapa, comEmail?: string) => {
    if (comEmail !== undefined) setEmail(comEmail);
    setEtapa(proxima);
  };

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
          {config.isError && <CaixaErro erro={config.error} />}

          {(etapa === 'entrar' || etapa === 'criar') && (
            <div className="segmentos" role="tablist" aria-label="Acesso" style={{ marginBottom: 20, width: '100%' }}>
              <button role="tab" aria-selected={etapa === 'entrar'} aria-pressed={etapa === 'entrar'} style={{ flex: 1 }} onClick={() => setEtapa('entrar')}>
                Entrar
              </button>
              <button role="tab" aria-selected={etapa === 'criar'} aria-pressed={etapa === 'criar'} style={{ flex: 1 }} onClick={() => setEtapa('criar')}>
                Criar conta
              </button>
            </div>
          )}

          {etapa === 'entrar' && <FormEntrar email={email} irPara={irPara} />}
          {etapa === 'criar' && <FormCriarConta dominios={dominios} email={email} irPara={irPara} />}
          {etapa === 'confirmar' && <FormCodigo email={email} finalidade="CADASTRO" irPara={irPara} />}
          {etapa === 'esqueci' && <FormEsqueci email={email} irPara={irPara} />}
          {etapa === 'redefinir' && <FormCodigo email={email} finalidade="REDEFINIR_SENHA" irPara={irPara} />}

          {config.data?.googleClientId && (etapa === 'entrar' || etapa === 'criar') && (
            <EntrarComGoogle clientId={config.data.googleClientId} />
          )}
        </div>
      </section>

      <ExemploAoVivo />
    </main>
  );
}

// ---- Formulários -------------------------------------------------------------------------

type IrPara = (etapa: Etapa, email?: string) => void;

function FormEntrar({ email: inicial, irPara }: { email: string; irPara: IrPara }) {
  const { concluirLogin } = useAutenticacao();
  const [email, setEmail] = useState(inicial);
  const [senha, setSenha] = useState('');
  const envio = useEnvio();

  const entrar = (e: FormEvent) => {
    e.preventDefault();
    envio.rodar(async () => {
      try {
        concluirLogin(await enviar<LoginResposta>('/api/auth/entrar', 'POST', { email, senha }));
      } catch (erro) {
        // Senha certa, e-mail ainda não confirmado: o servidor já mandou um código.
        if (erro instanceof ErroApi && erro.status === 403 && erro.titulo === 'E-mail não confirmado') {
          irPara('confirmar', email);
          return;
        }
        throw erro;
      }
    });
  };

  return (
    <form className="formulario" style={{ gap: 16 }} onSubmit={entrar} noValidate>
      <CampoEmail valor={email} onChange={setEmail} erro={envio.erros.email} />
      <CampoSenha id="senha" rotulo="Senha" valor={senha} onChange={setSenha} autoComplete="current-password" erro={envio.erros.senha} />
      <button type="button" className="link-botao" onClick={() => irPara('esqueci', email)}>
        Esqueci a senha
      </button>
      {envio.erroGeral != null && <CaixaErro erro={envio.erroGeral} />}
      <button className="botao botao-acao botao-grande" aria-busy={envio.ocupado}>
        {envio.ocupado ? 'Entrando…' : 'Entrar'}
      </button>
    </form>
  );
}

function FormCriarConta({ dominios, email: inicial, irPara }: { dominios: string[]; email: string; irPara: IrPara }) {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState(inicial);
  const [senha, setSenha] = useState('');
  const envio = useEnvio();
  const dicaDominio = dominios.length ? `Use o seu e-mail @${dominios.join(' ou @')}` : undefined;

  const criar = (e: FormEvent) => {
    e.preventDefault();
    const dominio = email.trim().toLowerCase().split('@')[1];
    if (dominios.length && dominio && !dominios.includes(dominio)) {
      envio.mostrarErros({ email: `${dicaDominio}.` });
      return;
    }
    envio.rodar(async () => {
      await enviar('/api/auth/cadastro', 'POST', { nome, email, senha });
      irPara('confirmar', email.trim().toLowerCase());
    });
  };

  return (
    <form className="formulario" style={{ gap: 16 }} onSubmit={criar} noValidate>
      <div className="campo">
        <label htmlFor="nome">Seu nome</label>
        <input
          id="nome"
          className="entrada"
          autoComplete="name"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          aria-invalid={Boolean(envio.erros.nome)}
        />
        {envio.erros.nome && <span className="erro">{envio.erros.nome}</span>}
      </div>
      <CampoEmail valor={email} onChange={setEmail} erro={envio.erros.email} dica={dicaDominio} rotulo="E-mail institucional" />
      <CampoSenha
        id="nova-senha"
        rotulo="Crie uma senha"
        valor={senha}
        onChange={setSenha}
        autoComplete="new-password"
        erro={envio.erros.senha}
        dica="Pelo menos 8 caracteres."
      />
      {envio.erroGeral != null && <CaixaErro erro={envio.erroGeral} />}
      <button className="botao botao-acao botao-grande" aria-busy={envio.ocupado}>
        {envio.ocupado ? 'Criando…' : 'Criar conta'}
      </button>
    </form>
  );
}

function FormEsqueci({ email: inicial, irPara }: { email: string; irPara: IrPara }) {
  const [email, setEmail] = useState(inicial);
  const envio = useEnvio();

  const pedir = (e: FormEvent) => {
    e.preventDefault();
    envio.rodar(async () => {
      await enviar('/api/auth/esqueci-senha', 'POST', { email });
      irPara('redefinir', email.trim().toLowerCase());
    });
  };

  return (
    <form className="formulario" style={{ gap: 16 }} onSubmit={pedir} noValidate>
      <Voltar onClick={() => irPara('entrar', email)} />
      <div>
        <h2>Trocar a senha</h2>
        <p className="secundario" style={{ marginTop: 6 }}>
          Mandamos um código para o seu e-mail institucional. Com ele você cria uma senha nova.
        </p>
      </div>
      <CampoEmail valor={email} onChange={setEmail} erro={envio.erros.email} />
      {envio.erroGeral != null && <CaixaErro erro={envio.erroGeral} />}
      <button className="botao botao-acao botao-grande" aria-busy={envio.ocupado}>
        {envio.ocupado ? 'Enviando…' : 'Enviar código'}
      </button>
    </form>
  );
}

/** Etapa do código de 6 números: confirma o cadastro ou, com senha nova, redefine a senha. */
function FormCodigo({ email, finalidade, irPara }: { email: string; finalidade: 'CADASTRO' | 'REDEFINIR_SENHA'; irPara: IrPara }) {
  const { concluirLogin } = useAutenticacao();
  const [codigo, setCodigo] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [espera, setEspera] = useState(60);
  const [avisoReenvio, setAvisoReenvio] = useState('');
  const envio = useEnvio();
  const cadastro = finalidade === 'CADASTRO';

  useEffect(() => {
    if (espera <= 0) return;
    const id = setTimeout(() => setEspera((s) => s - 1), 1000);
    return () => clearTimeout(id);
  }, [espera]);

  const confirmar = (e: FormEvent) => {
    e.preventDefault();
    envio.rodar(async () => {
      const resposta = cadastro
        ? await enviar<LoginResposta>('/api/auth/confirmar', 'POST', { email, codigo })
        : await enviar<LoginResposta>('/api/auth/redefinir-senha', 'POST', { email, codigo, novaSenha });
      concluirLogin(resposta);
    });
  };

  const reenviar = async () => {
    setEspera(60);
    setAvisoReenvio('');
    try {
      await enviar('/api/auth/reenviar-codigo', 'POST', { email, finalidade });
      setAvisoReenvio('Código novo a caminho. Confira também a caixa de spam.');
    } catch (erro) {
      setAvisoReenvio(erro instanceof Error ? erro.message : 'Não foi possível reenviar.');
    }
  };

  return (
    <form className="formulario" style={{ gap: 16 }} onSubmit={confirmar} noValidate>
      <Voltar onClick={() => irPara(cadastro ? 'criar' : 'esqueci', email)} />
      <div style={{ display: 'flex', gap: 14, alignItems: 'flex-start' }}>
        <span className="vazio-icone" style={{ margin: 0, width: 48, height: 48, flexShrink: 0 }} aria-hidden>
          <MailCheck size={24} />
        </span>
        <div>
          <h2>{cadastro ? 'Confirme o seu e-mail' : 'Crie uma senha nova'}</h2>
          <p className="secundario" style={{ marginTop: 6 }}>
            Enviamos um código de 6 números para <strong style={{ color: 'var(--tinta)' }}>{email}</strong>.
          </p>
        </div>
      </div>

      <div className="campo">
        <label htmlFor="codigo">Código</label>
        <input
          id="codigo"
          className="entrada entrada-codigo"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={7}
          placeholder="000000"
          value={codigo}
          onChange={(e) => setCodigo(e.target.value.replace(/[^\d ]/g, ''))}
          aria-invalid={Boolean(envio.erros.codigo)}
          autoFocus
        />
        {envio.erros.codigo && <span className="erro">{envio.erros.codigo}</span>}
      </div>

      {!cadastro && (
        <CampoSenha
          id="senha-nova"
          rotulo="Senha nova"
          valor={novaSenha}
          onChange={setNovaSenha}
          autoComplete="new-password"
          erro={envio.erros.novaSenha}
          dica="Pelo menos 8 caracteres."
        />
      )}

      {envio.erroGeral != null && <CaixaErro erro={envio.erroGeral} />}
      <button className="botao botao-acao botao-grande" aria-busy={envio.ocupado}>
        {envio.ocupado ? 'Conferindo…' : cadastro ? 'Confirmar e entrar' : 'Salvar senha e entrar'}
      </button>

      <p className="secundario" style={{ fontSize: 14 }} aria-live="polite">
        {avisoReenvio || 'Não chegou? '}
        {espera > 0 ? (
          <span className="numero">Você pode pedir outro em {espera}s.</span>
        ) : (
          <button type="button" className="link-botao" onClick={reenviar}>
            Reenviar código
          </button>
        )}
      </p>
    </form>
  );
}

function EntrarComGoogle({ clientId }: { clientId: string }) {
  const { entrarComGoogle } = useAutenticacao();
  const [erro, setErro] = useState<unknown>(null);
  return (
    <>
      <div className="divisor">ou</div>
      <GoogleOAuthProvider clientId={clientId}>
        <GoogleLogin
          onSuccess={(resposta) => {
            setErro(null);
            if (resposta.credential) entrarComGoogle(resposta.credential).catch(setErro);
          }}
          onError={() => setErro(new Error('Não foi possível entrar com o Google. Tente de novo.'))}
          text="continue_with"
          shape="pill"
          size="large"
          width="360"
          locale="pt-BR"
        />
      </GoogleOAuthProvider>
      {erro !== null && (
        <div style={{ marginTop: 12 }}>
          <CaixaErro erro={erro} />
        </div>
      )}
    </>
  );
}

// ---- Peças ------------------------------------------------------------------------------

function CampoEmail(props: { valor: string; onChange: (v: string) => void; erro?: string; dica?: string; rotulo?: string }) {
  return (
    <div className="campo">
      <label htmlFor="email">{props.rotulo ?? 'E-mail'}</label>
      <input
        id="email"
        className="entrada"
        type="email"
        inputMode="email"
        autoComplete="email"
        placeholder="voce@aluno.uepb.edu.br"
        value={props.valor}
        onChange={(e) => props.onChange(e.target.value)}
        aria-invalid={Boolean(props.erro)}
        aria-describedby={props.erro || props.dica ? 'email-ajuda' : undefined}
      />
      {(props.erro || props.dica) && (
        <span id="email-ajuda" className={props.erro ? 'erro' : 'dica'}>
          {props.erro ?? props.dica}
        </span>
      )}
    </div>
  );
}

function CampoSenha(props: {
  id: string;
  rotulo: string;
  valor: string;
  onChange: (v: string) => void;
  autoComplete: string;
  erro?: string;
  dica?: string;
}) {
  const [visivel, setVisivel] = useState(false);
  return (
    <div className="campo">
      <label htmlFor={props.id}>{props.rotulo}</label>
      <div style={{ position: 'relative' }}>
        <input
          id={props.id}
          className="entrada"
          type={visivel ? 'text' : 'password'}
          autoComplete={props.autoComplete}
          value={props.valor}
          onChange={(e) => props.onChange(e.target.value)}
          aria-invalid={Boolean(props.erro)}
          aria-describedby={props.erro || props.dica ? `${props.id}-ajuda` : undefined}
          style={{ paddingRight: 52 }}
        />
        <button
          type="button"
          className="botao botao-fantasma botao-icone"
          style={{ position: 'absolute', right: 2, top: 2 }}
          onClick={() => setVisivel(!visivel)}
          aria-label={visivel ? 'Esconder senha' : 'Mostrar senha'}
          aria-pressed={visivel}
        >
          {visivel ? <EyeOff size={18} aria-hidden /> : <Eye size={18} aria-hidden />}
        </button>
      </div>
      {(props.erro || props.dica) && (
        <span id={`${props.id}-ajuda`} className={props.erro ? 'erro' : 'dica'}>
          {props.erro ?? props.dica}
        </span>
      )}
    </div>
  );
}

function Voltar({ onClick }: { onClick: () => void }) {
  return (
    <button type="button" className="botao botao-fantasma botao-pequeno" style={{ alignSelf: 'flex-start', marginLeft: -10 }} onClick={onClick}>
      <ArrowLeft size={16} aria-hidden />
      Voltar
    </button>
  );
}

/** Estado comum dos formulários: ocupado, erros por campo (vindos da API) e erro geral. */
function useEnvio() {
  const [ocupado, setOcupado] = useState(false);
  const [erros, setErros] = useState<Record<string, string>>({});
  const [erroGeral, setErroGeral] = useState<unknown>(null);

  return {
    ocupado,
    erros,
    erroGeral,
    mostrarErros: (novos: Record<string, string>) => {
      setErros(novos);
      setErroGeral(null);
    },
    rodar: async (acao: () => Promise<void>) => {
      if (ocupado) return;
      setOcupado(true);
      setErros({});
      setErroGeral(null);
      try {
        await acao();
      } catch (erro) {
        if (erro instanceof ErroApi && Object.keys(erro.erros).length > 0) setErros(erro.erros);
        else setErroGeral(erro);
      } finally {
        setOcupado(false);
      }
    },
  };
}

// ---- Demonstração ----------------------------------------------------------------------

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
