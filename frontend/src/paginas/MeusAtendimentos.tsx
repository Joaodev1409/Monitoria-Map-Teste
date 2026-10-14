import { useState } from 'react';
import { Headset } from 'lucide-react';
import { useMeusAtendimentos } from '../api/consultas';
import { useUsuario } from '../auth/Autenticacao';
import { AcoesSolicitacao } from '../componentes/AcoesSolicitacao';
import { Cabecalho, CaixaErro, Carregando, Vazio } from '../componentes/Basicos';
import { LinhaSolicitacao, ListaSolicitacoes } from '../componentes/ItemSolicitacao';

type Filtro = 'EM_ATENDIMENTO' | 'CONCLUIDA';

export function MeusAtendimentos() {
  const usuario = useUsuario();
  const consulta = useMeusAtendimentos();
  const [filtro, setFiltro] = useState<Filtro>('EM_ATENDIMENTO');

  const todos = consulta.data ?? [];
  const contagem = (status: Filtro) => todos.filter((s) => s.status === status).length;
  const lista = todos.filter((s) => s.status === filtro);

  return (
    <>
      <Cabecalho
        titulo="Seus atendimentos"
        acoes={
          <div className="segmentos" role="group" aria-label="Filtrar atendimentos">
            <button aria-pressed={filtro === 'EM_ATENDIMENTO'} onClick={() => setFiltro('EM_ATENDIMENTO')}>
              Em andamento <span className="numero">· {contagem('EM_ATENDIMENTO')}</span>
            </button>
            <button aria-pressed={filtro === 'CONCLUIDA'} onClick={() => setFiltro('CONCLUIDA')}>
              Concluídos <span className="numero">· {contagem('CONCLUIDA')}</span>
            </button>
          </div>
        }
      >
        Os pedidos que você assumiu, do mais recente para o mais antigo.
      </Cabecalho>

      {consulta.isLoading && <Carregando />}
      {consulta.isError && <CaixaErro erro={consulta.error} />}
      {consulta.data && lista.length === 0 && (
        <Vazio
          icone={<Headset size={26} />}
          titulo={filtro === 'EM_ATENDIMENTO' ? 'Nada em andamento' : 'Nenhum atendimento concluído ainda'}
          texto={
            filtro === 'EM_ATENDIMENTO'
              ? 'Assuma um pedido na fila para começar.'
              : 'Os atendimentos que você concluir ficam registrados aqui.'
          }
          acao={filtro === 'EM_ATENDIMENTO' ? { rotulo: 'Ver a fila', para: '/monitor/fila' } : undefined}
        />
      )}
      {lista.length > 0 && (
        <ListaSolicitacoes
          itens={lista}
          render={(s, chegou) => (
            <LinhaSolicitacao
              key={s.id}
              solicitacao={s}
              pessoa="aluno"
              chegou={chegou}
              mostrarStatus={filtro === 'CONCLUIDA'}
              acoes={
                filtro === 'EM_ATENDIMENTO' ? (
                  <AcoesSolicitacao solicitacao={s} usuario={usuario} tamanho="pequeno" />
                ) : undefined
              }
            />
          )}
        />
      )}
    </>
  );
}
