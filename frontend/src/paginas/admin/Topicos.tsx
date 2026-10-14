import { useState, type FormEvent } from 'react';
import { Plus } from 'lucide-react';
import { useSalvarTopico, useTopicos } from '../../api/consultas';
import { Cabecalho, CaixaErro, Carregando } from '../../componentes/Basicos';
import { useAvisos } from '../../componentes/Avisos';

export function Topicos() {
  const consulta = useTopicos(true);
  const salvar = useSalvarTopico();
  const avisar = useAvisos();
  const [novo, setNovo] = useState('');
  const [editando, setEditando] = useState<{ id: number; nome: string } | null>(null);

  const criar = (e: FormEvent) => {
    e.preventDefault();
    if (!novo.trim()) return;
    salvar.mutate(
      { nome: novo.trim() },
      {
        onSuccess: () => {
          avisar(`Tópico "${novo.trim()}" criado.`);
          setNovo('');
        },
        onError: (erro) => avisar(erro.message, 'erro'),
      },
    );
  };

  const renomear = (e: FormEvent) => {
    e.preventDefault();
    if (!editando || !editando.nome.trim()) return;
    salvar.mutate(
      { id: editando.id, nome: editando.nome.trim() },
      { onSuccess: () => setEditando(null), onError: (erro) => avisar(erro.message, 'erro') },
    );
  };

  return (
    <>
      <Cabecalho titulo="Tópicos">
        Os assuntos que o aluno escolhe ao pedir ajuda. Desative em vez de apagar para manter o histórico.
      </Cabecalho>

      <form className="cartao cartao-corpo" onSubmit={criar} style={{ display: 'flex', gap: 10, marginBottom: 16, flexWrap: 'wrap' }}>
        <label htmlFor="novo-topico" className="sr-only">
          Nome do novo tópico
        </label>
        <input
          id="novo-topico"
          className="entrada"
          style={{ flex: '1 1 260px' }}
          maxLength={80}
          placeholder="Lista 4: Decorator e Composite"
          value={novo}
          onChange={(e) => setNovo(e.target.value)}
        />
        <button className="botao botao-acao" aria-busy={salvar.isPending}>
          <Plus size={18} aria-hidden />
          Adicionar tópico
        </button>
      </form>

      {consulta.isLoading && <Carregando />}
      {consulta.isError && <CaixaErro erro={consulta.error} />}
      {consulta.data && (
        <div className="cartao">
          <ul className="linhas">
            {consulta.data.map((t) => (
              <li key={t.id} className="linha">
                {editando?.id === t.id ? (
                  <form onSubmit={renomear} style={{ display: 'flex', gap: 8, flex: 1, flexWrap: 'wrap' }}>
                    <label htmlFor={`renomear-${t.id}`} className="sr-only">
                      Novo nome
                    </label>
                    <input
                      id={`renomear-${t.id}`}
                      className="entrada"
                      style={{ flex: '1 1 220px', minHeight: 40 }}
                      maxLength={80}
                      value={editando.nome}
                      onChange={(e) => setEditando({ id: t.id, nome: e.target.value })}
                      autoFocus
                    />
                    <button type="button" className="botao botao-fantasma botao-pequeno" onClick={() => setEditando(null)}>
                      Cancelar
                    </button>
                    <button className="botao botao-acao botao-pequeno">Salvar</button>
                  </form>
                ) : (
                  <>
                    <div className="linha-corpo">
                      <strong style={{ color: t.ativo ? undefined : 'var(--tinta-3)' }}>{t.nome}</strong>
                    </div>
                    <div className="linha-acoes">
                      {!t.ativo && <span className="selo selo-neutro">Inativo</span>}
                      <button className="botao botao-fantasma botao-pequeno" onClick={() => setEditando({ id: t.id, nome: t.nome })}>
                        Renomear
                      </button>
                      <button
                        className="botao botao-pequeno"
                        onClick={() =>
                          salvar.mutate({ id: t.id, ativo: !t.ativo }, { onError: (erro) => avisar(erro.message, 'erro') })
                        }
                      >
                        {t.ativo ? 'Desativar' : 'Reativar'}
                      </button>
                    </div>
                  </>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
}
