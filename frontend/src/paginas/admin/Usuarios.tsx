import { useMemo, useState } from 'react';
import { Search } from 'lucide-react';
import { useAlterarUsuario, useUsuarios } from '../../api/consultas';
import type { Papel } from '../../api/tipos';
import { useUsuario } from '../../auth/Autenticacao';
import { Avatar, Cabecalho, CaixaErro, Carregando } from '../../componentes/Basicos';
import { useAvisos } from '../../componentes/Avisos';

const rotuloPapel: Record<Papel, string> = { ALUNO: 'Aluno', MONITOR: 'Monitor', ADMIN: 'Administrador' };

export function Usuarios() {
  const eu = useUsuario();
  const consulta = useUsuarios();
  const alterar = useAlterarUsuario();
  const avisar = useAvisos();
  const [busca, setBusca] = useState('');
  const [filtro, setFiltro] = useState<Papel | 'TODOS'>('TODOS');

  const lista = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    return (consulta.data ?? []).filter(
      (u) =>
        (filtro === 'TODOS' || u.papel === filtro) &&
        (!termo || u.nome.toLowerCase().includes(termo) || u.email.toLowerCase().includes(termo)),
    );
  }, [consulta.data, busca, filtro]);

  const salvar = (id: number, mudanca: { papel?: Papel; ativo?: boolean }, sucesso: string) =>
    alterar.mutate({ id, ...mudanca }, { onSuccess: () => avisar(sucesso), onError: (e) => avisar(e.message, 'erro') });

  return (
    <>
      <Cabecalho titulo="Pessoas">
        Todo mundo entra como aluno no primeiro acesso. Promova aqui quem é monitor.
      </Cabecalho>

      <div className="filtros" style={{ marginBottom: 16 }}>
        <div className="campo" style={{ flex: '1 1 260px', position: 'relative' }}>
          <label htmlFor="busca" className="sr-only">
            Buscar por nome ou e-mail
          </label>
          <Search
            size={18}
            aria-hidden
            style={{ position: 'absolute', left: 14, top: 13, color: 'var(--tinta-3)' }}
          />
          <input
            id="busca"
            className="entrada"
            type="search"
            placeholder="Buscar por nome ou e-mail"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            style={{ width: '100%', paddingLeft: 42 }}
          />
        </div>
        <div className="segmentos" role="group" aria-label="Filtrar por papel">
          {(['TODOS', 'ALUNO', 'MONITOR', 'ADMIN'] as const).map((p) => (
            <button key={p} aria-pressed={filtro === p} onClick={() => setFiltro(p)}>
              {p === 'TODOS' ? 'Todos' : rotuloPapel[p]}
            </button>
          ))}
        </div>
      </div>

      {consulta.isLoading && <Carregando />}
      {consulta.isError && <CaixaErro erro={consulta.error} />}
      {consulta.data && (
        <div className="cartao">
          <ul className="linhas">
            {lista.length === 0 && (
              <li className="linha secundario" style={{ justifyContent: 'center', padding: 28 }}>
                Ninguém encontrado com esse filtro.
              </li>
            )}
            {lista.map((u) => (
              <li key={u.id} className="linha" style={{ opacity: u.ativo ? 1 : 0.6 }}>
                <Avatar nome={u.nome} foto={u.fotoUrl} />
                <div className="linha-corpo">
                  <strong className="linha-titulo">
                    {u.nome} {u.id === eu.id && <span className="secundario">(você)</span>}
                  </strong>
                  <span className="secundario" style={{ fontSize: 14, overflowWrap: 'anywhere' }}>
                    {u.email}
                  </span>
                </div>
                <div className="linha-acoes">
                  <label className="sr-only" htmlFor={`papel-${u.id}`}>
                    Papel de {u.nome}
                  </label>
                  <select
                    id={`papel-${u.id}`}
                    className="entrada"
                    style={{ minHeight: 38, padding: '4px 10px', fontSize: 14, width: 'auto' }}
                    value={u.papel}
                    disabled={u.id === eu.id}
                    onChange={(e) => {
                      const papel = e.target.value as Papel;
                      salvar(u.id, { papel }, `${u.nome} agora é ${rotuloPapel[papel].toLowerCase()}.`);
                    }}
                  >
                    {(['ALUNO', 'MONITOR', 'ADMIN'] as const).map((p) => (
                      <option key={p} value={p}>
                        {rotuloPapel[p]}
                      </option>
                    ))}
                  </select>
                  {u.id !== eu.id && (
                    <button
                      className="botao botao-fantasma botao-pequeno"
                      onClick={() =>
                        salvar(u.id, { ativo: !u.ativo }, u.ativo ? `${u.nome} foi desativado.` : `${u.nome} foi reativado.`)
                      }
                    >
                      {u.ativo ? 'Desativar' : 'Reativar'}
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
}
