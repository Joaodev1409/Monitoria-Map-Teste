import { useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Check, Hand, Undo2, X } from 'lucide-react';
import { chaves, useAcaoSolicitacao, type AcaoSolicitacao } from '../api/consultas';
import { comTransicao } from '../util/transicao';
import type { Solicitacao, Usuario } from '../api/tipos';
import { podeAtender } from '../auth/Autenticacao';
import { useAvisos } from './Avisos';

const mensagens: Record<AcaoSolicitacao, string> = {
  assumir: 'Atendimento assumido. O aluno já foi avisado.',
  concluir: 'Atendimento concluído.',
  devolver: 'Pedido devolvido para a fila.',
  cancelar: 'Pedido cancelado.',
};

/**
 * Mostra só os botões que fazem sentido para esta pessoa e este status.
 * O servidor valida tudo de novo (padrão State): aqui é só conveniência visual.
 * Ações que desfazem algo pedem confirmação ali mesmo, sem janela pop-up.
 */
export function AcoesSolicitacao({
  solicitacao: s,
  usuario,
  tamanho = 'normal',
}: {
  solicitacao: Solicitacao;
  usuario: Usuario;
  tamanho?: 'pequeno' | 'normal' | 'grande';
}) {
  const acao = useAcaoSolicitacao();
  const cliente = useQueryClient();
  const avisar = useAvisos();
  const [confirmando, setConfirmando] = useState<AcaoSolicitacao | null>(null);
  const [concluindo, setConcluindo] = useState(false);
  const [observacao, setObservacao] = useState('');

  const executar = (tipo: AcaoSolicitacao, extra?: { observacao?: string }) =>
    acao.mutate(
      { id: s.id, acao: tipo, ...extra },
      {
        onSuccess: (atualizada) => {
          avisar(mensagens[tipo]);
          setConfirmando(null);
          setConcluindo(false);
          if (tipo === 'assumir') {
            // O cartão sai da fila e "voa" para "Com você agora" (mesmo view-transition-name).
            comTransicao(() => {
              cliente.setQueryData<Solicitacao[]>(chaves.fila('ABERTA'), (lista) =>
                lista?.filter((x) => x.id !== atualizada.id),
              );
              cliente.setQueryData<Solicitacao[]>(chaves.atendimentos, (lista) => [
                atualizada,
                ...(lista ?? []).filter((x) => x.id !== atualizada.id),
              ]);
            });
          }
        },
        onError: (erro) => avisar(erro.message, 'erro'),
      },
    );

  const ehAluno = s.aluno.id === usuario.id;
  const ehResponsavel = s.monitor?.id === usuario.id || usuario.papel === 'ADMIN';
  const classe = tamanho === 'pequeno' ? 'botao-pequeno' : tamanho === 'grande' ? 'botao-grande' : '';
  const ocupado = acao.isPending;

  if (confirmando) {
    const textos: Partial<Record<AcaoSolicitacao, [string, string]>> = {
      cancelar: ['Cancelar este pedido?', 'Sim, cancelar'],
      devolver: ['Devolver para a fila?', 'Sim, devolver'],
    };
    const [pergunta, sim] = textos[confirmando] ?? ['Confirmar?', 'Confirmar'];
    return (
      <div className="confirmacao" role="group" aria-label={pergunta}>
        <span>{pergunta}</span>
        <button className="botao botao-pequeno botao-fantasma" onClick={() => setConfirmando(null)}>
          Não
        </button>
        <button
          className="botao botao-pequeno botao-perigo"
          aria-busy={ocupado}
          onClick={() => !ocupado && executar(confirmando)}
          autoFocus
        >
          {sim}
        </button>
      </div>
    );
  }

  if (s.status === 'ABERTA' && podeAtender(usuario.papel) && !ehAluno) {
    return (
      <Botao classe={`botao-acao ${classe}`} ocupado={ocupado} onClick={() => executar('assumir')}>
        <Hand size={tamanho === 'pequeno' ? 16 : 18} aria-hidden />
        {ocupado ? 'Assumindo…' : 'Assumir'}
      </Botao>
    );
  }

  if (s.status === 'ABERTA' && (ehAluno || usuario.papel === 'ADMIN')) {
    return (
      <Botao classe={`botao-perigo ${classe}`} ocupado={false} onClick={() => setConfirmando('cancelar')}>
        <X size={16} aria-hidden />
        Cancelar pedido
      </Botao>
    );
  }

  if (s.status === 'EM_ATENDIMENTO' && ehResponsavel && podeAtender(usuario.papel)) {
    if (concluindo) {
      return (
        <form
          style={{ display: 'flex', flexDirection: 'column', gap: 10, width: '100%' }}
          onSubmit={(e) => {
            e.preventDefault();
            executar('concluir', { observacao });
          }}
        >
          <label htmlFor={`observacao-${s.id}`} style={{ fontWeight: 700 }}>
            Como foi o atendimento? <span className="secundario">(opcional)</span>
          </label>
          <textarea
            id={`observacao-${s.id}`}
            className="entrada"
            style={{ minHeight: 96 }}
            maxLength={2000}
            placeholder="O que foi resolvido, material indicado, próximo passo do aluno"
            value={observacao}
            onChange={(e) => setObservacao(e.target.value)}
            autoFocus
          />
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', flexWrap: 'wrap' }}>
            <button type="button" className="botao botao-fantasma" onClick={() => setConcluindo(false)}>
              Voltar
            </button>
            <button type="submit" className="botao botao-acao" aria-busy={ocupado}>
              <Check size={18} aria-hidden />
              {ocupado ? 'Concluindo…' : 'Concluir atendimento'}
            </button>
          </div>
        </form>
      );
    }
    return (
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <Botao classe={`botao-fantasma ${classe}`} ocupado={false} onClick={() => setConfirmando('devolver')}>
          <Undo2 size={16} aria-hidden />
          Devolver
        </Botao>
        <Botao classe={`botao-acao ${classe}`} ocupado={false} onClick={() => setConcluindo(true)}>
          <Check size={17} aria-hidden />
          Concluir
        </Botao>
      </div>
    );
  }

  return null;
}

function Botao(props: { classe: string; ocupado: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button
      className={`botao ${props.classe}`}
      aria-busy={props.ocupado}
      onClick={() => !props.ocupado && props.onClick()}
    >
      {props.children}
    </button>
  );
}
