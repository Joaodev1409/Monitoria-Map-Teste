import { useEffect, useState } from 'react';
import { RotateCw } from 'lucide-react';
import { SimboloMarca } from './Marca';

/** A partir de quando cada mensagem aparece (segundos de espera). */
const MOSTRAR_TEXTO = 2;
const EXPLICAR_ACORDANDO = 7;
const SUGERIR_RECARREGAR = 75;

/**
 * Tela de abertura enquanto o app descobre se há sessão. Normalmente some em menos de um
 * segundo; mas no plano gratuito a API "dorme" depois de um tempo sem uso e leva até um
 * minuto para acordar. Em vez de uma tela parada, uma aurora de luz verde e âmbar passeia
 * no fundo, os pontos do símbolo pulsam e o texto explica o que está acontecendo conforme
 * a espera aumenta. Nada de barra de progresso inventada: não sabemos quanto falta.
 */
export function Abertura() {
  const segundos = useSegundos();

  return (
    <main className="abertura">
      <div className="aurora" aria-hidden>
        <span />
        <span />
        <span />
      </div>
      <SimboloMarca tamanho={52} />
      <div className="abertura-texto" role="status" aria-live="polite">
        {segundos < MOSTRAR_TEXTO ? (
          <span className="sr-only">Abrindo a monitoria…</span>
        ) : segundos < EXPLICAR_ACORDANDO ? (
          <p className="abertura-titulo">Abrindo a monitoria…</p>
        ) : segundos < SUGERIR_RECARREGAR ? (
          <>
            <p className="abertura-titulo">Acordando o servidor…</p>
            <p className="secundario">
              Quando ninguém usa o sistema por um tempo, ele dorme. A primeira entrada pode levar até um minuto;
              depois disso fica rápido.
            </p>
          </>
        ) : (
          <>
            <p className="abertura-titulo">Está demorando mais que o normal.</p>
            <p className="secundario">Confira a sua internet e tente de novo.</p>
            <button className="botao botao-suave" style={{ marginTop: 16 }} onClick={() => window.location.reload()}>
              <RotateCw size={18} aria-hidden />
              Recarregar
            </button>
          </>
        )}
      </div>
    </main>
  );
}

/** Segundos desde que o componente apareceu. */
function useSegundos() {
  const [segundos, setSegundos] = useState(0);
  useEffect(() => {
    const inicio = Date.now();
    const t = setInterval(() => setSegundos(Math.floor((Date.now() - inicio) / 1000)), 1000);
    return () => clearInterval(t);
  }, []);
  return segundos;
}
