import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { CircleCheck, CircleAlert, Info } from 'lucide-react';

type TipoAviso = 'sucesso' | 'erro' | 'info';
interface Aviso {
  id: number;
  texto: string;
  tipo: TipoAviso;
}

const Contexto = createContext<(texto: string, tipo?: TipoAviso) => void>(() => {});

/** Mensagens rápidas no canto da tela ("toasts"), anunciadas para leitores de tela. */
export function ProvedorAvisos({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([]);

  const avisar = useCallback((texto: string, tipo: TipoAviso = 'sucesso') => {
    const id = Date.now() + Math.random();
    setAvisos((atuais) => [...atuais.slice(-2), { id, texto, tipo }]);
    setTimeout(() => setAvisos((atuais) => atuais.filter((a) => a.id !== id)), tipo === 'erro' ? 6000 : 4000);
  }, []);

  return (
    <Contexto.Provider value={avisar}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((aviso) => (
          <div key={aviso.id} className={`aviso ${aviso.tipo === 'erro' ? 'aviso-erro' : ''}`}>
            {aviso.tipo === 'erro' ? (
              <CircleAlert size={18} aria-hidden />
            ) : aviso.tipo === 'info' ? (
              <Info size={18} aria-hidden />
            ) : (
              <CircleCheck size={18} aria-hidden />
            )}
            <span>{aviso.texto}</span>
          </div>
        ))}
      </div>
    </Contexto.Provider>
  );
}

export const useAvisos = () => useContext(Contexto);
