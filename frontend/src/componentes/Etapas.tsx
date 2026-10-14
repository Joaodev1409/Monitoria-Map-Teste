import { useEffect, useState, type CSSProperties } from 'react';
import { Check, Headset, Hourglass } from 'lucide-react';
import type { Solicitacao } from '../api/tipos';
import { haQuanto, primeiroNome } from '../util/formatar';
import { useAgora } from '../util/useAgora';

/**
 * Onde o pedido está agora: Na fila → Com o monitor → Concluído.
 *
 * É a interação assinatura do app: quando o servidor avisa (SSE) que o pedido mudou,
 * a barra avança com uma mola suave e a etapa atual "chama" com um pulso.
 */
export function Etapas({ solicitacao: s }: { solicitacao: Solicitacao }) {
  const agora = useAgora();
  const indice = s.status === 'ABERTA' ? 0 : s.status === 'EM_ATENDIMENTO' ? 1 : 2;
  const alvo = s.status === 'CONCLUIDA' ? 1 : indice / 2;

  // Começa em zero e avança no quadro seguinte: a barra "anda" ao abrir e a cada mudança.
  const [progresso, setProgresso] = useState(0);
  useEffect(() => {
    const quadro = requestAnimationFrame(() => setProgresso(alvo));
    return () => cancelAnimationFrame(quadro);
  }, [alvo]);

  const etapas = [
    {
      nome: 'Na fila',
      detalhe: s.status === 'ABERTA' ? haQuanto(s.criadaEm, agora) : null,
      icone: <Hourglass size={16} strokeWidth={2.25} />,
    },
    {
      nome: s.monitor ? `Com ${primeiroNome(s.monitor.nome)}` : 'Com o monitor',
      detalhe: s.status === 'ABERTA' ? 'aguardando' : s.status === 'EM_ATENDIMENTO' ? 'atendendo agora' : null,
      icone: <Headset size={16} strokeWidth={2.25} />,
    },
    {
      nome: 'Concluído',
      detalhe: null,
      icone: <Check size={17} strokeWidth={2.5} />,
    },
  ];

  return (
    <ol className="etapas" style={{ '--progresso': progresso } as CSSProperties} aria-label="Andamento do pedido">
      {etapas.map((etapa, i) => {
        const feita = i < indice || s.status === 'CONCLUIDA';
        const atual = i === indice && s.status !== 'CONCLUIDA';
        return (
          <li
            key={i}
            className={`etapa ${feita ? 'feita' : ''} ${atual ? 'atual' : ''}`}
            aria-current={atual ? 'step' : undefined}
          >
            <span className="etapa-ponto" aria-hidden>
              {feita ? <Check size={17} strokeWidth={2.75} /> : etapa.icone}
            </span>
            <span>
              {etapa.nome}
              {etapa.detalhe && <small>{etapa.detalhe}</small>}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
