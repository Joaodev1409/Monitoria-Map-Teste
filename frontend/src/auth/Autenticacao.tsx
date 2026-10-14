import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { api, enviar, quandoSessaoExpirar } from '../api/cliente';
import type { LoginResposta, Papel, Usuario } from '../api/tipos';

interface ContextoAutenticacao {
  usuario: Usuario | null;
  carregando: boolean;
  /** Chame com a resposta de qualquer rota que faz login (entrar, confirmar, redefinir, Google). */
  concluirLogin: (resposta: LoginResposta) => void;
  entrarComGoogle: (credential: string) => Promise<void>;
  sair: () => Promise<void>;
}

const Contexto = createContext<ContextoAutenticacao | null>(null);

/**
 * Quem está logado. O token mora num cookie HttpOnly que o JavaScript não lê:
 * para saber se há sessão, perguntamos ao servidor ao abrir o app.
 */
export function ProvedorAutenticacao({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(true);
  const clienteQuery = useQueryClient();

  const esquecer = useCallback(() => {
    setUsuario(null);
    clienteQuery.clear();
  }, [clienteQuery]);

  useEffect(() => {
    quandoSessaoExpirar(esquecer);
    // Descobre se já existe sessão (e recebe o cookie CSRF antes do primeiro POST).
    // Sempre responde 200: visitante deslogado não gera erro 401 no console.
    api<{ usuario: Usuario | null }>('/api/auth/sessao')
      .then((r) => setUsuario(r.usuario))
      .catch(() => setUsuario(null))
      .finally(() => setCarregando(false));
  }, [esquecer]);

  const valor = useMemo<ContextoAutenticacao>(
    () => ({
      usuario,
      carregando,
      concluirLogin: (resposta) => setUsuario(resposta.usuario),
      entrarComGoogle: async (credential) => {
        const r = await enviar<LoginResposta>('/api/auth/google', 'POST', { credential });
        setUsuario(r.usuario);
      },
      sair: async () => {
        await enviar('/api/auth/sair', 'POST').catch(() => undefined);
        esquecer();
      },
    }),
    [usuario, carregando, esquecer],
  );

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useAutenticacao() {
  const contexto = useContext(Contexto);
  if (!contexto) throw new Error('useAutenticacao precisa estar dentro de <ProvedorAutenticacao>');
  return contexto;
}

/** Usuário logado (só use dentro de rotas protegidas). */
export function useUsuario(): Usuario {
  const { usuario } = useAutenticacao();
  if (!usuario) throw new Error('Nenhum usuário logado');
  return usuario;
}

export const podeAtender = (papel: Papel) => papel === 'MONITOR' || papel === 'ADMIN';
