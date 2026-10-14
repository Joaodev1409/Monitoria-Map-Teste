import { useState, type FormEvent } from 'react';
import { ExternalLink, Link2, Pencil } from 'lucide-react';
import { ErroApi } from '../api/cliente';
import { useDefinirLink } from '../api/consultas';
import type { Solicitacao, Usuario } from '../api/tipos';
import { useAvisos } from './Avisos';

/** Aceita só links http(s): nunca renderiza "javascript:" ou outros esquemas como link. */
export const linkSeguro = (url: string | null) => (url && /^https?:\/\//i.test(url) ? url : null);

/**
 * Espelha a regra do servidor (padrão State): enquanto está na fila, só o aluno mexe no link;
 * durante o atendimento, o aluno e o monitor responsável (ou um admin).
 */
export function podeEditarLink(s: Solicitacao, usuario: Usuario) {
  if (s.modalidade !== 'ONLINE') return false;
  const ehAluno = s.aluno.id === usuario.id;
  const ehAdmin = usuario.papel === 'ADMIN';
  if (s.status === 'ABERTA') return ehAluno || ehAdmin;
  if (s.status === 'EM_ATENDIMENTO') return ehAluno || ehAdmin || s.monitor?.id === usuario.id;
  return false;
}

/** Bloco do link da chamada: mostra, deixa adicionar quando falta e trocar quando já existe. */
export function LinkReuniao({ solicitacao: s, usuario }: { solicitacao: Solicitacao; usuario: Usuario }) {
  const link = linkSeguro(s.linkReuniao);
  const editavel = podeEditarLink(s, usuario);
  const [editando, setEditando] = useState(false);

  if (s.modalidade !== 'ONLINE') return null;

  if (!link || editando) {
    if (!editavel) {
      return <p className="secundario">Ainda sem link. O monitor envia quando for começar.</p>;
    }
    return <FormLink solicitacao={s} inicial={s.linkReuniao ?? ''} aoTerminar={() => setEditando(false)} podeCancelar={Boolean(link)} />;
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
      <a href={link} target="_blank" rel="noreferrer noopener" className="botao botao-suave">
        <ExternalLink size={17} aria-hidden />
        Entrar na chamada
      </a>
      {editavel && (
        <button className="botao botao-fantasma botao-pequeno" onClick={() => setEditando(true)}>
          <Pencil size={15} aria-hidden />
          Trocar link
        </button>
      )}
    </div>
  );
}

function FormLink(props: { solicitacao: Solicitacao; inicial: string; podeCancelar: boolean; aoTerminar: () => void }) {
  const definir = useDefinirLink();
  const avisar = useAvisos();
  const [valor, setValor] = useState(props.inicial);
  const [erro, setErro] = useState('');

  const salvar = (e: FormEvent) => {
    e.preventDefault();
    const limpo = valor.trim();
    if (!/^https?:\/\/\S+$/i.test(limpo)) {
      setErro('Cole o link completo, começando com https://');
      return;
    }
    definir.mutate(
      { id: props.solicitacao.id, linkReuniao: limpo },
      {
        onSuccess: () => {
          avisar('Link salvo. Quem está no pedido já pode entrar na chamada.');
          props.aoTerminar();
        },
        onError: (falha) => setErro(falha instanceof ErroApi ? falha.erros.linkReuniao ?? falha.message : 'Não foi possível salvar.'),
      },
    );
  };

  const id = `link-${props.solicitacao.id}`;
  return (
    <form onSubmit={salvar} className="campo" noValidate>
      <label htmlFor={id} className="rotulo-campo">
        Link da chamada
      </label>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <div style={{ position: 'relative', flex: '1 1 220px' }}>
          <Link2 size={18} aria-hidden style={{ position: 'absolute', left: 14, top: 15, color: 'var(--tinta-3)' }} />
          <input
            id={id}
            className="entrada"
            type="url"
            inputMode="url"
            placeholder="https://meet.google.com/abc-defg-hij"
            value={valor}
            onChange={(e) => {
              setValor(e.target.value);
              setErro('');
            }}
            aria-invalid={Boolean(erro)}
            aria-describedby={erro ? `${id}-erro` : undefined}
            style={{ paddingLeft: 42 }}
          />
        </div>
        <button className="botao botao-acao" aria-busy={definir.isPending}>
          {definir.isPending ? 'Salvando…' : 'Salvar link'}
        </button>
        {props.podeCancelar && (
          <button type="button" className="botao botao-fantasma" onClick={props.aoTerminar}>
            Cancelar
          </button>
        )}
      </div>
      {erro && (
        <span id={`${id}-erro`} className="erro">
          {erro}
        </span>
      )}
    </form>
  );
}
