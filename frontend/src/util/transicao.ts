type DocumentoComTransicao = Document & {
  startViewTransition?: (atualizar: () => Promise<void>) => unknown;
};

/**
 * Envolve uma mudança de tela numa View Transition: o navegador fotografa o antes e o depois
 * e anima entre os dois. Elementos com o mesmo `view-transition-name` nos dois estados
 * (ex.: o cartão na fila e a linha em "Com você agora") deslizam de um lugar para o outro.
 *
 * Sem suporte no navegador, ou com "reduzir movimento" ligado, a mudança acontece direto.
 */
export function comTransicao(atualizar: () => void) {
  const doc = document as DocumentoComTransicao;
  const reduzir = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (!doc.startViewTransition || reduzir) {
    atualizar();
    return;
  }
  doc.startViewTransition(async () => {
    atualizar();
    // O React desenha a mudança numa tarefa seguinte; esperamos ela antes da foto do "depois".
    await new Promise((pronto) => setTimeout(pronto, 50));
  });
}

/** Nome estável para o mesmo pedido em telas diferentes. */
export const nomeTransicao = (id: number) => `pedido-${id}`;
