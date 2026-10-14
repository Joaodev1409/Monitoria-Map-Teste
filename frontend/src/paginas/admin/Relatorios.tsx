import { useState } from 'react';
import { Download } from 'lucide-react';
import { baixarArquivo } from '../../api/cliente';
import { useRelatorio } from '../../api/consultas';
import type { Contagem } from '../../api/tipos';
import { Cabecalho, CaixaErro, Carregando } from '../../componentes/Basicos';
import { useAvisos } from '../../componentes/Avisos';
import { dataIso, duracao, rotuloStatus } from '../../util/formatar';

const formatoSemana = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' });

export function Relatorios() {
  const hoje = new Date();
  const [de, setDe] = useState(dataIso(new Date(hoje.getFullYear(), hoje.getMonth(), 1)));
  const [ate, setAte] = useState(dataIso(hoje));
  const consulta = useRelatorio(de, ate);
  const avisar = useAvisos();
  const r = consulta.data;

  const exportar = () =>
    baixarArquivo(`/api/relatorios/export.csv?de=${de}&ate=${ate}`, 'monitoria-map.csv').catch((e: Error) =>
      avisar(e.message, 'erro'),
    );

  return (
    <>
      <Cabecalho
        titulo="Relatórios"
        acoes={
          <div className="filtros">
            <div className="campo">
              <label htmlFor="de" style={{ fontSize: 13 }}>
                De
              </label>
              <input id="de" type="date" className="entrada" value={de} max={ate} onChange={(e) => setDe(e.target.value)} />
            </div>
            <div className="campo">
              <label htmlFor="ate" style={{ fontSize: 13 }}>
                Até
              </label>
              <input id="ate" type="date" className="entrada" value={ate} min={de} onChange={(e) => setAte(e.target.value)} />
            </div>
            <button className="botao botao-suave" onClick={exportar}>
              <Download size={18} aria-hidden />
              Exportar CSV
            </button>
          </div>
        }
      >
        Números da monitoria no período, para o relatório e para a turma.
      </Cabecalho>

      {consulta.isLoading && <Carregando linhas={2} />}
      {consulta.isError && <CaixaErro erro={consulta.error} />}
      {r && (
        <div className="pilha">
          <dl className="cartao ficha" style={{ margin: 0 }}>
            <div>
              <dt>Pedidos</dt>
              <dd>{r.total}</dd>
            </div>
            <div>
              <dt>Concluídos</dt>
              <dd>{r.porStatus.CONCLUIDA}</dd>
            </div>
            <div>
              <dt>Espera média</dt>
              <dd>{duracao(r.tempoMedioEsperaMinutos)}</dd>
            </div>
            <div>
              <dt>Duração média</dt>
              <dd>{duracao(r.tempoMedioAtendimentoMinutos)}</dd>
            </div>
          </dl>

          <div className="grade-2">
            <section className="cartao cartao-corpo">
              <h2>Assuntos mais pedidos</h2>
              <Barras dados={r.porTopico} vazio="Nenhum pedido no período." />
            </section>

            <section className="cartao cartao-corpo">
              <h2>Pedidos por semana</h2>
              <Barras
                dados={r.porSemana.map((s) => ({
                  rotulo: `Semana de ${formatoSemana.format(new Date(`${s.rotulo}T12:00:00`))}`,
                  total: s.total,
                }))}
                vazio="Nenhum pedido no período."
              />
            </section>

            <section className="cartao">
              <div className="cartao-corpo" style={{ paddingBottom: 6 }}>
                <h2>Atendimentos por monitor</h2>
              </div>
              {r.porMonitor.length === 0 ? (
                <p className="cartao-corpo secundario" style={{ paddingTop: 0 }}>
                  Nenhum atendimento concluído no período.
                </p>
              ) : (
                <div className="tabela-wrapper">
                  <table className="tabela">
                    <thead>
                      <tr>
                        <th scope="col">Monitor</th>
                        <th scope="col" style={{ textAlign: 'right' }}>
                          Concluídos
                        </th>
                        <th scope="col" style={{ textAlign: 'right' }}>
                          Duração média
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {r.porMonitor.map((m) => (
                        <tr key={m.monitorId}>
                          <td style={{ fontWeight: 700 }}>{m.nome}</td>
                          <td className="numero" style={{ textAlign: 'right' }}>
                            {m.atendimentosConcluidos}
                          </td>
                          <td className="numero" style={{ textAlign: 'right' }}>
                            {duracao(m.tempoMedioAtendimentoMinutos)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <section className="cartao cartao-corpo">
              <h2>Status e modalidade</h2>
              <Barras
                dados={(['CONCLUIDA', 'EM_ATENDIMENTO', 'ABERTA', 'CANCELADA'] as const).map((s) => ({
                  rotulo: rotuloStatus[s],
                  total: r.porStatus[s],
                }))}
                vazio=""
              />
              <p className="secundario" style={{ marginTop: 18 }}>
                {r.porModalidade.PRESENCIAL} presenciais · {r.porModalidade.ONLINE} online
              </p>
            </section>
          </div>
        </div>
      )}
    </>
  );
}

/** Barras horizontais simples: legíveis no celular e sem biblioteca de gráficos. */
function Barras({ dados, vazio }: { dados: Contagem[]; vazio: string }) {
  const maximo = Math.max(1, ...dados.map((d) => d.total));
  if (dados.length === 0 || dados.every((d) => d.total === 0)) {
    return vazio ? (
      <p className="secundario" style={{ marginTop: 12 }}>
        {vazio}
      </p>
    ) : null;
  }
  return (
    <div className="barras">
      {dados.map((d) => (
        <div key={d.rotulo} className="barra-linha">
          <span>{d.rotulo}</span>
          <strong className="numero">{d.total}</strong>
          <div className="barra-trilha" aria-hidden>
            <div className="barra-preenchida" style={{ width: `${(d.total / maximo) * 100}%` }} />
          </div>
        </div>
      ))}
    </div>
  );
}
