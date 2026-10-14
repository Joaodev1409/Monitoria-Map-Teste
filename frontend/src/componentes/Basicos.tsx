import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { CircleAlert, CircleCheck, CircleX, Clock3, Headset } from 'lucide-react';
import type { StatusSolicitacao } from '../api/tipos';
import { iniciais, rotuloStatus } from '../util/formatar';

const iconeStatus: Record<StatusSolicitacao, ReactNode> = {
  ABERTA: <Clock3 size={15} strokeWidth={2.25} aria-hidden />,
  EM_ATENDIMENTO: <Headset size={15} strokeWidth={2.25} aria-hidden />,
  CONCLUIDA: <CircleCheck size={15} strokeWidth={2.25} aria-hidden />,
  CANCELADA: <CircleX size={15} strokeWidth={2.25} aria-hidden />,
};

export function SeloStatus({ status }: { status: StatusSolicitacao }) {
  return (
    <span className={`selo selo-${status}`}>
      {iconeStatus[status]}
      {rotuloStatus[status]}
    </span>
  );
}

export function Avatar({ nome, foto, tamanho = 40 }: { nome: string; foto?: string | null; tamanho?: number }) {
  return (
    <span
      className="avatar"
      style={{ width: tamanho, height: tamanho, borderRadius: tamanho * 0.36, fontSize: tamanho * 0.36 }}
      aria-hidden
    >
      {foto ? <img src={foto} alt="" referrerPolicy="no-referrer" /> : iniciais(nome)}
    </span>
  );
}

/** Esqueleto no formato do conteúdo que vai chegar (em vez de um spinner solto). */
export function Carregando({ linhas = 3, texto = 'Carregando…' }: { linhas?: number; texto?: string }) {
  return (
    <div className="cartao" role="status" aria-live="polite">
      <span className="sr-only">{texto}</span>
      <ul className="linhas" aria-hidden>
        {Array.from({ length: linhas }, (_, i) => (
          <li key={i} className="linha">
            <span className="esqueleto" style={{ width: 40, height: 40, borderRadius: 14 }} />
            <div className="linha-corpo">
              <div className="esqueleto" style={{ height: 14, width: `${70 - i * 12}%` }} />
              <div className="esqueleto" style={{ height: 12, width: '40%', marginTop: 10 }} />
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function Vazio(props: {
  icone: ReactNode;
  titulo: string;
  texto: string;
  acao?: { rotulo: string; para: string };
}) {
  return (
    <div className="cartao vazio">
      <div className="vazio-icone" aria-hidden>
        {props.icone}
      </div>
      <h2>{props.titulo}</h2>
      <p>{props.texto}</p>
      {props.acao && (
        <Link className="botao botao-acao" to={props.acao.para}>
          {props.acao.rotulo}
        </Link>
      )}
    </div>
  );
}

export function CaixaErro({ erro }: { erro: unknown }) {
  const mensagem = erro instanceof Error ? erro.message : 'Algo deu errado.';
  return (
    <div className="erro-caixa" role="alert">
      <CircleAlert size={18} aria-hidden style={{ flexShrink: 0, marginTop: 2 }} />
      {mensagem}
    </div>
  );
}

export function Cabecalho({ titulo, children, acoes }: { titulo: ReactNode; children?: ReactNode; acoes?: ReactNode }) {
  return (
    <header className="cabecalho">
      <div>
        <h1>{titulo}</h1>
        {children && <p>{children}</p>}
      </div>
      {acoes}
    </header>
  );
}
