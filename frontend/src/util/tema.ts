import { useEffect, useState } from 'react';

export type Tema = 'sistema' | 'claro' | 'escuro';
const CHAVE = 'monitoria.tema';

function lerTema(): Tema {
  try {
    const salvo = localStorage.getItem(CHAVE);
    return salvo === 'claro' || salvo === 'escuro' ? salvo : 'sistema';
  } catch {
    return 'sistema';
  }
}

/** Claro, escuro ou seguir o sistema. Aplica em <html data-tema="..."> e lembra a escolha. */
export function useTema(): [Tema, (tema: Tema) => void] {
  const [tema, setTema] = useState<Tema>(lerTema);

  useEffect(() => {
    const raiz = document.documentElement;
    if (tema === 'sistema') raiz.removeAttribute('data-tema');
    else raiz.setAttribute('data-tema', tema);
    try {
      localStorage.setItem(CHAVE, tema);
    } catch {
      /* ignorado */
    }
  }, [tema]);

  return [tema, setTema];
}
