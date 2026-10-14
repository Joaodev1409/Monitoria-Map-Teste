import type { ReactNode } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { ChartColumn, Headset, House, Inbox, Plus, UserRound } from 'lucide-react';
import { useFila } from '../api/consultas';
import type { Usuario } from '../api/tipos';
import { podeAtender, useUsuario } from '../auth/Autenticacao';
import { useTempoReal } from '../tempo-real/useTempoReal';
import { Avatar } from './Basicos';
import { SimboloMarca } from './Marca';

interface Destino {
  para: string;
  rotulo: string;
  icone: ReactNode;
  contador?: number;
  central?: boolean;
}

/** Os destinos de cada papel. No celular viram a barra inferior; no notebook, o menu do topo. */
function destinos(usuario: Usuario, aguardando: number): Destino[] {
  if (!podeAtender(usuario.papel)) {
    return [
      { para: '/inicio', rotulo: 'Início', icone: <House size={22} strokeWidth={2} /> },
      { para: '/pedir-ajuda', rotulo: 'Pedir ajuda', icone: <Plus size={28} strokeWidth={2.5} />, central: true },
      { para: '/conta', rotulo: 'Conta', icone: <UserRound size={22} strokeWidth={2} /> },
    ];
  }
  const lista: Destino[] = [
    { para: '/monitor/fila', rotulo: 'Fila', icone: <Inbox size={22} strokeWidth={2} />, contador: aguardando },
    { para: '/monitor/atendimentos', rotulo: 'Atendimentos', icone: <Headset size={22} strokeWidth={2} /> },
  ];
  if (usuario.papel === 'ADMIN') {
    lista.push({ para: '/admin', rotulo: 'Gestão', icone: <ChartColumn size={22} strokeWidth={2} /> });
  }
  lista.push({ para: '/conta', rotulo: 'Conta', icone: <UserRound size={22} strokeWidth={2} /> });
  return lista;
}

export function Layout() {
  const usuario = useUsuario();
  const conectado = useTempoReal(usuario);
  const monitor = podeAtender(usuario.papel);
  const fila = useFila('ABERTA', monitor);
  const aguardando = fila.data?.filter((s) => s.aluno.id !== usuario.id).length ?? 0;
  const itens = destinos(usuario, aguardando);

  return (
    <>
      <a href="#conteudo" className="sr-only">
        Pular para o conteúdo
      </a>
      <header className="topo">
        <div className="topo-linha">
          <NavLink to="/" className="marca" aria-label="Monitoria MAP, início">
            <SimboloMarca />
            Monitoria MAP
          </NavLink>
          <nav className="nav-topo" aria-label="Principal">
            {itens
              .filter((d) => d.para !== '/conta')
              .map((d) => (
                <NavLink key={d.para} to={d.para}>
                  {d.rotulo}
                  {!!d.contador && (
                    <span className="contador" aria-label={`${d.contador} esperando`}>
                      {d.contador}
                    </span>
                  )}
                </NavLink>
              ))}
          </nav>
          <div className="topo-acoes">
            <span
              className={`ao-vivo ${conectado ? '' : 'desligado'}`}
              role="status"
              title={conectado ? 'Atualizando ao vivo' : 'Reconectando…'}
            >
              <i aria-hidden />
              <span className="rotulo">{conectado ? 'Ao vivo' : 'Reconectando'}</span>
              <span className="sr-only">{conectado ? 'Atualizando ao vivo' : 'Reconectando'}</span>
            </span>
            <NavLink to="/conta" className="conta-atalho" aria-label={`Sua conta, ${usuario.nome}`}>
              <Avatar nome={usuario.nome} foto={usuario.fotoUrl} tamanho={38} />
            </NavLink>
          </div>
        </div>
      </header>

      <main id="conteudo" className="conteudo">
        <Outlet />
      </main>

      <nav className="barra-inferior" aria-label="Principal (celular)">
        {itens.map((d) => (
          <NavLink key={d.para} to={d.para} className={d.central ? 'botao-central' : undefined}>
            {d.icone}
            <span>{d.rotulo}</span>
            {!!d.contador && (
              <span className="contador" aria-label={`${d.contador} esperando`}>
                {d.contador}
              </span>
            )}
          </NavLink>
        ))}
      </nav>
    </>
  );
}
