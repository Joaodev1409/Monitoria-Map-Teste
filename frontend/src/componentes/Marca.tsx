/**
 * Símbolo do Monitoria MAP: uma fila de três pessoas, e a primeira (a próxima a ser
 * atendida) acesa na cor de ação. Desenhado em SVG para ficar nítido em qualquer tamanho.
 */
export function SimboloMarca({ tamanho = 34 }: { tamanho?: number }) {
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 34 34" aria-hidden focusable="false">
      <rect width="34" height="34" rx="11" fill="var(--tinta)" />
      <circle cx="10" cy="17" r="4.2" fill="var(--acao)" />
      <circle cx="19" cy="17" r="3.2" fill="var(--fundo)" opacity="0.9" />
      <circle cx="26.5" cy="17" r="2.4" fill="var(--fundo)" opacity="0.55" />
    </svg>
  );
}
