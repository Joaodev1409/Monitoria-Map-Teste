import type { ReactNode } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import type { Papel } from './api/tipos';
import { podeAtender, useAutenticacao } from './auth/Autenticacao';
import { Layout } from './componentes/Layout';
import { SimboloMarca } from './componentes/Marca';
import { Conta } from './paginas/Conta';
import { DetalheSolicitacao } from './paginas/DetalheSolicitacao';
import { Fila } from './paginas/Fila';
import { InicioAluno } from './paginas/InicioAluno';
import { Login } from './paginas/Login';
import { MeusAtendimentos } from './paginas/MeusAtendimentos';
import { PedirAjuda } from './paginas/PedirAjuda';
import { Privacidade } from './paginas/Privacidade';
import { Gestao } from './paginas/admin/Gestao';
import { Relatorios } from './paginas/admin/Relatorios';
import { Topicos } from './paginas/admin/Topicos';
import { Usuarios } from './paginas/admin/Usuarios';

/** Página inicial de cada papel. */
const inicio = (papel: Papel) => (podeAtender(papel) ? '/monitor/fila' : '/inicio');

function Exige({ papeis, children }: { papeis: Papel[]; children: ReactNode }) {
  const { usuario } = useAutenticacao();
  if (!usuario) return null;
  return papeis.includes(usuario.papel) ? <>{children}</> : <Navigate to={inicio(usuario.papel)} replace />;
}

const MONITORES: Papel[] = ['MONITOR', 'ADMIN'];

export function App() {
  const { usuario, carregando } = useAutenticacao();

  if (carregando) {
    return (
      <div className="carregando" role="status" style={{ minHeight: '100vh' }}>
        <SimboloMarca tamanho={44} />
        <span className="sr-only">Abrindo a monitoria…</span>
      </div>
    );
  }

  return (
    <BrowserRouter>
      {!usuario ? (
        <Routes>
          <Route path="privacidade" element={<Privacidade />} />
          <Route path="*" element={<Login />} />
        </Routes>
      ) : (
        <Routes>
          <Route path="privacidade" element={<Privacidade />} />
          <Route element={<Layout />}>
            <Route index element={<Navigate to={inicio(usuario.papel)} replace />} />
            <Route path="inicio" element={<InicioAluno />} />
            <Route path="minhas" element={<Navigate to="/inicio" replace />} />
            <Route path="pedir-ajuda" element={<PedirAjuda />} />
            <Route path="solicitacoes/:id" element={<DetalheSolicitacao />} />
            <Route path="conta" element={<Conta />} />
            <Route path="monitor/fila" element={<Exige papeis={MONITORES}><Fila /></Exige>} />
            <Route path="monitor/atendimentos" element={<Exige papeis={MONITORES}><MeusAtendimentos /></Exige>} />
            <Route path="admin" element={<Exige papeis={['ADMIN']}><Gestao /></Exige>}>
              <Route index element={<Navigate to="relatorios" replace />} />
              <Route path="relatorios" element={<Relatorios />} />
              <Route path="pessoas" element={<Usuarios />} />
              <Route path="topicos" element={<Topicos />} />
            </Route>
            <Route path="*" element={<Navigate to={inicio(usuario.papel)} replace />} />
          </Route>
        </Routes>
      )}
    </BrowserRouter>
  );
}
