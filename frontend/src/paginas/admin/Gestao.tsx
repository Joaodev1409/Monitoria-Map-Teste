import { NavLink, Outlet } from 'react-router-dom';

/** Área do admin: relatórios, pessoas e tópicos lado a lado num controle segmentado. */
export function Gestao() {
  return (
    <>
      <nav className="segmentos" aria-label="Gestão" style={{ marginBottom: 24 }}>
        <SubLink para="/admin/relatorios">Relatórios</SubLink>
        <SubLink para="/admin/pessoas">Pessoas</SubLink>
        <SubLink para="/admin/topicos">Tópicos</SubLink>
      </nav>
      <Outlet />
    </>
  );
}

function SubLink({ para, children }: { para: string; children: string }) {
  return (
    <NavLink to={para} className="segmento-link">
      {children}
    </NavLink>
  );
}
