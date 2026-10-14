import { useEffect, useState } from 'react';

/** Re-renderiza a cada intervalo, para textos como "há 3 min" continuarem certos. */
export function useAgora(intervaloMs = 30_000): number {
  const [agora, setAgora] = useState(Date.now);
  useEffect(() => {
    const id = setInterval(() => setAgora(Date.now()), intervaloMs);
    return () => clearInterval(id);
  }, [intervaloMs]);
  return agora;
}
