import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ErroApi } from './api/cliente';
import { App } from './App';
import { ProvedorAutenticacao } from './auth/Autenticacao';
import { ProvedorAvisos } from './componentes/Avisos';
import './estilos.css';

const cliente = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      // Erros 4xx (sem permissão, não encontrado) não melhoram tentando de novo.
      retry: (falhas, erro) => !(erro instanceof ErroApi && erro.status >= 400 && erro.status < 500) && falhas < 2,
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={cliente}>
      <ProvedorAvisos>
        <ProvedorAutenticacao>
          <App />
        </ProvedorAutenticacao>
      </ProvedorAvisos>
    </QueryClientProvider>
  </StrictMode>,
);
