import { Link } from 'react-router-dom';
import { Clock3, Coffee, MapPin, Video } from 'lucide-react';
import { useFila, useMeusAtendimentos } from '../api/consultas';
import type { Solicitacao } from '../api/tipos';
import { useUsuario } from '../auth/Autenticacao';
import { AcoesSolicitacao } from '../componentes/AcoesSolicitacao';
import { Avatar, Cabecalho, CaixaErro, Carregando, Vazio } from '../componentes/Basicos';
import { LinhaSolicitacao, ListaSolicitacoes, semLink } from '../componentes/ItemSolicitacao';
import { haQuanto, primeiroNome, rotuloModalidade } from '../util/formatar';
import { nomeTransicao } from '../util/transicao';
import { useAgora } from '../util/useAgora';

/** Fila de pedidos abertos: quem espera há mais tempo vem primeiro, em destaque. */
export function Fila() {
  const usuario = useUsuario();
  const agora = useAgora();
  const consulta = useFila('ABERTA');
  const fila = consulta.data?.filter((s) => s.aluno.id !== usuario.id) ?? [];
  const [proximo, ...depois] = fila;

  const titulo =
    fila.length === 0 ? 'Fila vazia' : fila.length === 1 ? '1 aluno esperando' : `${fila.length} alunos esperando`;

  return (
    <div className="colunas">
      <div>
        <Cabecalho titulo={consulta.data ? titulo : 'Fila'}>
          {proximo
            ? `${fraseEspera(primeiroNome(proximo.aluno.nome), proximo.criadaEm, agora)} Quem chega primeiro é atendido primeiro.`
            : consulta.data
              ? 'Quando alguém pedir ajuda, o pedido aparece aqui na hora.'
              : undefined}
        </Cabecalho>

        {consulta.isLoading && <Carregando />}
        {consulta.isError && <CaixaErro erro={consulta.error} />}
        {consulta.data && !proximo && (
          <Vazio
            icone={<Coffee size={26} />}
            titulo="Ninguém esperando agora"
            texto="Deixe esta aba aberta: um pedido novo chega com som e notificação, se os alertas estiverem ligados na sua conta."
          />
        )}

        {proximo && <ProximoDaFila solicitacao={proximo} />}

        {depois.length > 0 && (
          <>
            <div className="secao-titulo">
              <h2>Na sequência</h2>
              <span className="secundario numero">{depois.length}</span>
            </div>
            <ListaSolicitacoes
              itens={depois}
              render={(s, chegou) => (
                <LinhaSolicitacao
                  key={s.id}
                  solicitacao={s}
                  pessoa="aluno"
                  chegou={chegou}
                  mostrarStatus={false}
                  acoes={<AcoesSolicitacao solicitacao={s} usuario={usuario} tamanho="pequeno" />}
                />
              )}
            />
          </>
        )}
      </div>

      <ComVoceAgora />
    </div>
  );
}

function fraseEspera(nome: string, desde: string, agora: number) {
  const quanto = haQuanto(desde, agora);
  return quanto === 'agora mesmo' ? `${nome} acabou de entrar na fila.` : `${nome} espera ${quanto}.`;
}

function ProximoDaFila({ solicitacao: s }: { solicitacao: Solicitacao }) {
  const usuario = useUsuario();
  const agora = useAgora();
  return (
    <article
      key={s.id}
      className="cartao cartao-corpo chegou clicavel"
      aria-labelledby={`proximo-${s.id}`}
      style={{ viewTransitionName: nomeTransicao(s.id) }}
    >
      <div className="destaque">
        <div style={{ minWidth: 0 }}>
          <div className="pessoa" style={{ marginBottom: 16 }}>
            <Avatar nome={s.aluno.nome} foto={s.aluno.fotoUrl} tamanho={44} />
            <div>
              <strong>{s.aluno.nome}</strong>
              <small className="espera">
                <Clock3 size={13} aria-hidden />
                {haQuanto(s.criadaEm, agora) === 'agora mesmo'
                  ? 'chegou agora'
                  : `esperando ${haQuanto(s.criadaEm, agora).replace('há ', '')}`}
              </small>
            </div>
          </div>
          <Link id={`proximo-${s.id}`} to={`/solicitacoes/${s.id}`} className="destaque-titulo esticado">
            {s.titulo}
          </Link>
          <p className="destaque-descricao">{s.descricao}</p>
          <p className="meta" style={{ marginTop: 12 }}>
            <span>{s.topico.nome}</span>
            <span>
              {s.modalidade === 'ONLINE' ? <Video size={14} aria-hidden /> : <MapPin size={14} aria-hidden />}
              {rotuloModalidade[s.modalidade]}
              {semLink(s) && <span className="sem-link">· sem link</span>}
            </span>
          </p>
        </div>
        <AcoesSolicitacao solicitacao={s} usuario={usuario} tamanho="grande" />
      </div>
    </article>
  );
}

/** Coluna lateral: o que já está com este monitor e um resumo honesto do dia dele. */
function ComVoceAgora() {
  const usuario = useUsuario();
  const consulta = useMeusAtendimentos();
  const emAndamento = consulta.data?.filter((s) => s.status === 'EM_ATENDIMENTO') ?? [];
  const hoje = new Date().toDateString();
  const concluidosHoje =
    consulta.data?.filter((s) => s.concluidaEm && new Date(s.concluidaEm).toDateString() === hoje).length ?? 0;

  return (
    <aside className="pilha" aria-label="Seus atendimentos">
      <section className="cartao">
        <div className="cartao-corpo" style={{ paddingBottom: emAndamento.length ? 8 : undefined }}>
          <h2>Com você agora</h2>
          {emAndamento.length === 0 && (
            <p className="secundario" style={{ marginTop: 6 }}>
              Nenhum atendimento em andamento.
            </p>
          )}
        </div>
        {emAndamento.length > 0 && (
          <ul className="linhas" style={{ paddingTop: 0 }}>
            {emAndamento.map((s) => (
              <li key={s.id} className="linha" style={{ flexWrap: 'wrap', viewTransitionName: nomeTransicao(s.id) }}>
                <Avatar nome={s.aluno.nome} foto={s.aluno.fotoUrl} tamanho={36} />
                <div className="linha-corpo">
                  <Link to={`/solicitacoes/${s.id}`} className="linha-titulo">
                    {s.titulo}
                  </Link>
                  <span className="secundario" style={{ fontSize: 14 }}>
                    {s.aluno.nome}
                    {semLink(s) && <span className="sem-link"> · falta o link da chamada</span>}
                  </span>
                </div>
                <div style={{ width: '100%', paddingLeft: 50 }}>
                  <AcoesSolicitacao solicitacao={s} usuario={usuario} tamanho="pequeno" />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="cartao cartao-corpo">
        <h2>Seu dia</h2>
        <p className="secundario" style={{ marginTop: 6 }}>
          {concluidosHoje === 0
            ? 'Você ainda não concluiu atendimentos hoje.'
            : concluidosHoje === 1
              ? 'Você concluiu 1 atendimento hoje.'
              : `Você concluiu ${concluidosHoje} atendimentos hoje.`}
        </p>
        <Link to="/monitor/atendimentos" className="botao botao-fantasma botao-pequeno" style={{ marginTop: 12, marginLeft: -10 }}>
          Ver todos os atendimentos
        </Link>
      </section>
    </aside>
  );
}
