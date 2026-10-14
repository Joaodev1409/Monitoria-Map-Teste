import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Clock3, MapPin, Tag, Video } from 'lucide-react';
import type { Solicitacao } from '../api/tipos';
import { haQuanto, rotuloModalidade, tempoRelativo } from '../util/formatar';
import { nomeTransicao } from '../util/transicao';
import { useAgora } from '../util/useAgora';
import { Avatar, SeloStatus } from './Basicos';

interface PropsLinha {
  solicitacao: Solicitacao;
  /** Quem aparece no avatar: o aluno (visão do monitor) ou o monitor (visão do aluno). */
  pessoa: 'aluno' | 'monitor';
  acoes?: ReactNode;
  mostrarStatus?: boolean;
  chegou?: boolean;
}

export function LinhaSolicitacao({ solicitacao: s, pessoa, acoes, mostrarStatus = true, chegou }: PropsLinha) {
  const agora = useAgora();
  const quem = pessoa === 'aluno' ? s.aluno : s.monitor;
  const esperando = s.status === 'ABERTA';

  return (
    <li className={`linha ${chegou ? 'chegou' : ''}`} style={{ viewTransitionName: nomeTransicao(s.id) }}>
      {quem ? <Avatar nome={quem.nome} foto={quem.fotoUrl} /> : <Avatar nome="?" />}
      <div className="linha-corpo">
        <Link to={`/solicitacoes/${s.id}`} className="linha-titulo">
          {s.titulo}
        </Link>
        <div className="meta">
          {quem && <span>{pessoa === 'monitor' ? `com ${quem.nome}` : quem.nome}</span>}
          <span>
            <Tag size={14} aria-hidden />
            {s.topico.nome}
          </span>
          <span>
            {s.modalidade === 'ONLINE' ? <Video size={14} aria-hidden /> : <MapPin size={14} aria-hidden />}
            {rotuloModalidade[s.modalidade]}
            {semLink(s) && <span className="sem-link">· sem link</span>}
          </span>
          {esperando ? (
            <span className="espera">
              <Clock3 size={14} aria-hidden />
              <time dateTime={s.criadaEm}>{haQuanto(s.criadaEm, agora)}</time>
            </span>
          ) : (
            <span>
              <time dateTime={s.criadaEm}>{tempoRelativo(s.criadaEm, agora)}</time>
            </span>
          )}
        </div>
      </div>
      {(mostrarStatus || acoes) && (
        <div className="linha-acoes">
          {mostrarStatus && <SeloStatus status={s.status} />}
          {acoes}
        </div>
      )}
      <ChevronRight size={18} className="linha-seta" aria-hidden />
    </li>
  );
}

/** Atendimento online ainda em andamento e sem link da chamada. */
export const semLink = (s: Solicitacao) =>
  s.modalidade === 'ONLINE' && !s.linkReuniao && (s.status === 'ABERTA' || s.status === 'EM_ATENDIMENTO');

/**
 * Lista de pedidos dentro de um único cartão. Lembra quais ids já mostrou: o que chegar
 * depois (pela conexão ao vivo) entra deslizando com um brilho verde rápido.
 */
export function ListaSolicitacoes({
  itens,
  render,
}: {
  itens: Solicitacao[];
  render: (s: Solicitacao, chegou: boolean) => ReactNode;
}) {
  const conhecidos = useRef<Set<number> | null>(null);
  const [recentes, setRecentes] = useState<Set<number>>(() => new Set());
  const chave = itens.map((s) => s.id).join(',');

  useEffect(() => {
    const ids = itens.map((s) => s.id);
    if (conhecidos.current === null) {
      conhecidos.current = new Set(ids);
      return;
    }
    const novos = ids.filter((id) => !conhecidos.current!.has(id));
    ids.forEach((id) => conhecidos.current!.add(id));
    if (novos.length === 0) return;
    setRecentes(new Set(novos));
    const t = setTimeout(() => setRecentes(new Set()), 1400);
    return () => clearTimeout(t);
  }, [chave]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="cartao">
      <ul className="linhas">{itens.map((s) => render(s, recentes.has(s.id)))}</ul>
    </div>
  );
}
