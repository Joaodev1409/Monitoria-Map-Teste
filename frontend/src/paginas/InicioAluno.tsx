import { Link } from 'react-router-dom';
import { ArrowRight, MapPin, Plus, Video } from 'lucide-react';
import { useMinhasSolicitacoes, useTopicos } from '../api/consultas';
import type { Solicitacao } from '../api/tipos';
import { useUsuario } from '../auth/Autenticacao';
import { AcoesSolicitacao } from '../componentes/AcoesSolicitacao';
import { Avatar, Cabecalho, CaixaErro, Carregando, SeloStatus } from '../componentes/Basicos';
import { Etapas } from '../componentes/Etapas';
import { LinhaSolicitacao, ListaSolicitacoes } from '../componentes/ItemSolicitacao';
import { primeiroNome, rotuloModalidade } from '../util/formatar';

export function InicioAluno() {
  const usuario = useUsuario();
  const consulta = useMinhasSolicitacoes();
  const todas = consulta.data ?? [];
  const ativas = todas.filter((s) => s.status === 'ABERTA' || s.status === 'EM_ATENDIMENTO');
  const historico = todas.filter((s) => s.status === 'CONCLUIDA' || s.status === 'CANCELADA');

  return (
    <>
      <Cabecalho
        titulo={`Oi, ${primeiroNome(usuario.nome)}`}
        acoes={
          ativas.length > 0 && (
            <Link to="/pedir-ajuda" className="botao botao-suave">
              <Plus size={18} aria-hidden />
              Novo pedido
            </Link>
          )
        }
      >
        {ativas.length === 0
          ? 'Travou em algum exercício ou conceito de MAP? Um monitor te ajuda.'
          : ativas.some((s) => s.status === 'EM_ATENDIMENTO')
            ? 'Um monitor está com você. Acompanhe por aqui.'
            : 'Os monitores já foram avisados. Esta tela atualiza sozinha.'}
      </Cabecalho>

      {consulta.isLoading && <Carregando linhas={2} />}
      {consulta.isError && <CaixaErro erro={consulta.error} />}

      {consulta.data && ativas.length === 0 && <ConvitePedirAjuda />}
      {ativas.length > 0 && (
        <div className="pilha">
          {ativas.map((s) => (
            <PedidoEmAndamento key={s.id} solicitacao={s} />
          ))}
        </div>
      )}

      {historico.length > 0 && (
        <>
          <div className="secao-titulo">
            <h2>Pedidos anteriores</h2>
            <span className="secundario">{historico.length}</span>
          </div>
          <ListaSolicitacoes
            itens={historico}
            render={(s, chegou) => <LinhaSolicitacao key={s.id} solicitacao={s} pessoa="monitor" chegou={chegou} />}
          />
        </>
      )}
    </>
  );
}

function PedidoEmAndamento({ solicitacao: s }: { solicitacao: Solicitacao }) {
  const usuario = useUsuario();
  return (
    <article className="cartao cartao-corpo clicavel">
      <div className="titulo-com-selo">
        <div style={{ minWidth: 0 }}>
          <Link to={`/solicitacoes/${s.id}`} className="destaque-titulo esticado">
            {s.titulo}
          </Link>
          <p className="meta" style={{ marginTop: 6 }}>
            <span>{s.topico.nome}</span>
            <span>
              {s.modalidade === 'ONLINE' ? <Video size={14} aria-hidden /> : <MapPin size={14} aria-hidden />}
              {rotuloModalidade[s.modalidade]}
            </span>
          </p>
        </div>
        <SeloStatus status={s.status} />
      </div>

      <div style={{ margin: '28px 0 8px' }}>
        <Etapas solicitacao={s} />
      </div>

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: 12,
          flexWrap: 'wrap',
          marginTop: 22,
          paddingTop: 18,
          borderTop: '1px solid var(--borda)',
        }}
      >
        {s.monitor ? (
          <div className="pessoa">
            <Avatar nome={s.monitor.nome} foto={s.monitor.fotoUrl} />
            <div>
              <strong>{s.monitor.nome}</strong>
              <small>está cuidando do seu pedido</small>
            </div>
          </div>
        ) : (
          <AcoesSolicitacao solicitacao={s} usuario={usuario} tamanho="pequeno" />
        )}
        <Link to={`/solicitacoes/${s.id}`} className="botao botao-fantasma botao-pequeno">
          Ver detalhes
          <ArrowRight size={16} aria-hidden />
        </Link>
      </div>
    </article>
  );
}

/** Estado sem pedidos: um convite claro, com atalhos para os tópicos mais comuns. */
function ConvitePedirAjuda() {
  const topicos = useTopicos();
  return (
    <section className="cartao cartao-corpo" aria-labelledby="convite-titulo">
      <div className="destaque">
        <div>
          <h2 id="convite-titulo" className="destaque-titulo">
            Qual é a dúvida de hoje?
          </h2>
          <p className="destaque-descricao">
            Conte o que travou. Todos os monitores recebem o aviso na hora, e você vê aqui quando um deles assumir.
          </p>
        </div>
        <Link to="/pedir-ajuda" className="botao botao-acao botao-grande">
          <Plus size={20} aria-hidden />
          Pedir ajuda
        </Link>
      </div>

      {topicos.data && topicos.data.length > 0 && (
        <div style={{ marginTop: 24 }}>
          <p className="secundario" style={{ fontSize: 14, fontWeight: 600, marginBottom: 10 }}>
            Ou comece por um assunto
          </p>
          <div className="pilulas">
            {topicos.data.slice(0, 8).map((t) => (
              <Link key={t.id} to={`/pedir-ajuda?topico=${t.id}`} className="pilula">
                {t.nome}
              </Link>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}
