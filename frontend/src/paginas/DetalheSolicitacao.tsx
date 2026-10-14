import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, MapPin, Video } from 'lucide-react';
import { useSolicitacao } from '../api/consultas';
import { podeAtender, useUsuario } from '../auth/Autenticacao';
import { AcoesSolicitacao } from '../componentes/AcoesSolicitacao';
import { Avatar, CaixaErro, Carregando, SeloStatus } from '../componentes/Basicos';
import { Etapas } from '../componentes/Etapas';
import { LinkReuniao, podeEditarLink } from '../componentes/LinkReuniao';
import { duracao, formatarDataHora, rotuloEvento, rotuloModalidade } from '../util/formatar';

const minutosEntre = (inicio: string | null, fim: string | null) =>
  inicio && fim ? (new Date(fim).getTime() - new Date(inicio).getTime()) / 60_000 : null;

export function DetalheSolicitacao() {
  const id = Number(useParams().id);
  const usuario = useUsuario();
  const consulta = useSolicitacao(id);

  if (consulta.isLoading) return <Carregando linhas={2} />;
  if (consulta.isError) return <CaixaErro erro={consulta.error} />;
  const s = consulta.data!;
  const voltar = podeAtender(usuario.papel) && s.aluno.id !== usuario.id ? '/monitor/fila' : '/inicio';
  const faltaLink = s.modalidade === 'ONLINE' && !s.linkReuniao && podeEditarLink(s, usuario);

  return (
    <>
      <Link to={voltar} className="botao botao-fantasma botao-pequeno" style={{ marginLeft: -10, marginBottom: 14 }}>
        <ArrowLeft size={16} aria-hidden />
        Voltar
      </Link>

      <div className="detalhe">
        <div className="pilha">
          <article className="cartao cartao-corpo">
            <div className="titulo-com-selo">
              <h1 style={{ fontSize: 'clamp(1.4rem, 1.2rem + 1vw, 1.9rem)', overflowWrap: 'anywhere' }}>{s.titulo}</h1>
              <SeloStatus status={s.status} />
            </div>
            <p className="meta" style={{ marginTop: 8 }}>
              <span>{s.topico.nome}</span>
              <span>
                {s.modalidade === 'ONLINE' ? <Video size={14} aria-hidden /> : <MapPin size={14} aria-hidden />}
                {rotuloModalidade[s.modalidade]}
              </span>
              <span>
                aberto em <time dateTime={s.criadaEm}>{formatarDataHora(s.criadaEm)}</time>
              </span>
            </p>

            {s.status !== 'CANCELADA' && (
              <div style={{ margin: '30px 0 6px' }}>
                <Etapas solicitacao={s} />
              </div>
            )}

            {s.modalidade === 'ONLINE' && s.status !== 'CANCELADA' && s.status !== 'CONCLUIDA' && (
              <section className={faltaLink ? 'aviso-link' : undefined} style={{ marginTop: 28 }} aria-labelledby="chamada-titulo">
                <h2 id="chamada-titulo">Chamada online</h2>
                {faltaLink && (
                  <p className="secundario" style={{ margin: '4px 0 12px' }}>
                    {s.aluno.id === usuario.id
                      ? 'Se já tiver um link, cole aqui. Se não, o monitor envia quando assumir.'
                      : 'O aluno não mandou link. Cole o link da chamada para ele entrar.'}
                  </p>
                )}
                <div style={{ marginTop: faltaLink ? 0 : 10 }}>
                  <LinkReuniao solicitacao={s} usuario={usuario} />
                </div>
              </section>
            )}

            <h2 style={{ marginTop: 30 }}>A dúvida</h2>
            <p className="descricao" style={{ marginTop: 8 }}>
              {s.descricao}
            </p>

            {s.observacaoConclusao && (
              <>
                <h2 style={{ marginTop: 26 }}>O que o monitor anotou</h2>
                <p className="descricao" style={{ marginTop: 8 }}>
                  {s.observacaoConclusao}
                </p>
              </>
            )}

            <AreaDeAcoes>
              <AcoesSolicitacao solicitacao={s} usuario={usuario} />
            </AreaDeAcoes>
          </article>

          <section className="cartao cartao-corpo" aria-labelledby="historico-titulo">
            <h2 id="historico-titulo">Histórico</h2>
            <ol className="historico">
              {s.historico.map((evento, i) => (
                <li key={i}>
                  <strong>{evento.autor.nome}</strong> {rotuloEvento[evento.tipo]}
                  {evento.detalhe && evento.tipo === 'DEVOLVIDA' && <span className="secundario"> · {evento.detalhe}</span>}
                  <time dateTime={evento.ocorridoEm}>{formatarDataHora(evento.ocorridoEm)}</time>
                </li>
              ))}
            </ol>
          </section>
        </div>

        <aside className="cartao cartao-corpo">
          <dl className="dados">
            <div>
              <dt>Aluno</dt>
              <dd className="pessoa" style={{ marginTop: 8 }}>
                <Avatar nome={s.aluno.nome} foto={s.aluno.fotoUrl} tamanho={34} />
                <strong>{s.aluno.nome}</strong>
              </dd>
            </div>
            <div>
              <dt>Monitor</dt>
              <dd className="pessoa" style={{ marginTop: 8 }}>
                {s.monitor ? (
                  <>
                    <Avatar nome={s.monitor.nome} foto={s.monitor.fotoUrl} tamanho={34} />
                    <strong>{s.monitor.nome}</strong>
                  </>
                ) : (
                  <span className="secundario" style={{ fontWeight: 400 }}>
                    Ainda ninguém. Todos foram avisados.
                  </span>
                )}
              </dd>
            </div>
            <div>
              <dt>Tópico</dt>
              <dd>{s.topico.nome}</dd>
            </div>
            <div>
              <dt>Modalidade</dt>
              <dd>{rotuloModalidade[s.modalidade]}</dd>
            </div>
            <div>
              <dt>Aberto em</dt>
              <dd className="numero">{formatarDataHora(s.criadaEm)}</dd>
            </div>
            {s.assumidaEm && (
              <div>
                <dt>Assumido em</dt>
                <dd className="numero">{formatarDataHora(s.assumidaEm)}</dd>
              </div>
            )}
            {s.concluidaEm && (
              <div>
                <dt>Concluído em</dt>
                <dd className="numero">{formatarDataHora(s.concluidaEm)}</dd>
              </div>
            )}
            {s.assumidaEm && (
              <div>
                <dt>Espera até alguém assumir</dt>
                <dd className="numero">{duracao(minutosEntre(s.criadaEm, s.assumidaEm))}</dd>
              </div>
            )}
            {s.concluidaEm && (
              <div>
                <dt>Duração do atendimento</dt>
                <dd className="numero">{duracao(minutosEntre(s.assumidaEm, s.concluidaEm))}</dd>
              </div>
            )}
          </dl>
        </aside>
      </div>
    </>
  );
}

/** Só desenha a faixa de ações quando existe alguma ação para esta pessoa. */
function AreaDeAcoes({ children }: { children: ReactNode }) {
  return (
    <div className="area-acoes" style={{ marginTop: 26, paddingTop: 20, borderTop: '1px solid var(--borda)' }}>
      {children}
    </div>
  );
}
