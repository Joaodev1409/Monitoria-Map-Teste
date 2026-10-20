import { useState } from 'react';
import { Link } from 'react-router-dom';
import { LogOut, Monitor, Moon, Sun } from 'lucide-react';
import { podeAtender, useAutenticacao, useUsuario } from '../auth/Autenticacao';
import { Avatar, Cabecalho } from '../componentes/Basicos';
import { useAvisos } from '../componentes/Avisos';
import { definirSom, pedirPermissao, somAtivado, suportaNotificacoes } from '../tempo-real/notificacoes';
import { useTema, type Tema } from '../util/tema';

const papeis = { ALUNO: 'Aluno', MONITOR: 'Monitor', ADMIN: 'Administrador' } as const;

export function Conta() {
  const usuario = useUsuario();
  const { sair } = useAutenticacao();
  const [tema, setTema] = useTema();
  const [saindo, setSaindo] = useState(false);

  return (
    <div style={{ maxWidth: 640 }}>
      <Cabecalho titulo="Sua conta" />

      <section className="cartao cartao-corpo">
        <div className="pessoa">
          <Avatar nome={usuario.nome} foto={usuario.fotoUrl} tamanho={56} />
          {/* O papel fica embaixo do e-mail (e não à direita): no celular, nome e e-mail longos
              não deixavam espaço e o selo empurrava a tela para o lado. */}
          <div>
            <strong style={{ fontSize: 18 }}>{usuario.nome}</strong>
            <small>{usuario.email}</small>
            <span className="selo selo-neutro" style={{ marginTop: 8 }}>
              {papeis[usuario.papel]}
            </span>
          </div>
        </div>
      </section>

      <div className="secao-titulo">
        <h2>Preferências</h2>
      </div>
      <section className="cartao">
        <ul className="linhas">
          <li className="linha" style={{ flexWrap: 'wrap' }}>
            {/* Base de 220px: sem espaço para o texto e os botões lado a lado, os botões descem. */}
            <div className="linha-corpo" style={{ flexBasis: 220 }}>
              <strong>Aparência</strong>
              <p className="secundario" style={{ fontSize: 14 }}>
                Siga o celular ou escolha claro ou escuro.
              </p>
            </div>
            <div className="segmentos segmentos-cheio" role="group" aria-label="Aparência">
              {(
                [
                  ['sistema', Monitor, 'Sistema'],
                  ['claro', Sun, 'Claro'],
                  ['escuro', Moon, 'Escuro'],
                ] as [Tema, typeof Sun, string][]
              ).map(([valor, Icone, rotulo]) => (
                <button key={valor} aria-pressed={tema === valor} onClick={() => setTema(valor)}>
                  <Icone size={15} aria-hidden style={{ verticalAlign: -2, marginRight: 6 }} />
                  {rotulo}
                </button>
              ))}
            </div>
          </li>
          {podeAtender(usuario.papel) && <AlertasDeFila />}
        </ul>
      </section>

      <button
        className="botao botao-perigo"
        style={{ marginTop: 28 }}
        aria-busy={saindo}
        onClick={async () => {
          setSaindo(true);
          await sair();
        }}
      >
        <LogOut size={18} aria-hidden />
        {saindo ? 'Saindo…' : 'Sair da conta'}
      </button>
      <p className="secundario" style={{ fontSize: 14, marginTop: 10 }}>
        Sair encerra a sessão em todos os aparelhos. <Link to="/privacidade">Política de privacidade</Link>
      </p>
    </div>
  );
}

function AlertasDeFila() {
  const [ativo, setAtivo] = useState(somAtivado);
  const [permissao, setPermissao] = useState(() =>
    suportaNotificacoes() ? Notification.permission : ('denied' as NotificationPermission),
  );
  const avisar = useAvisos();

  return (
    <>
      <li className="linha">
        <div className="linha-corpo">
          <strong id="rotulo-som">Som de pedido novo</strong>
          <p className="secundario" style={{ fontSize: 14 }}>
            Um toque curto quando alguém entra na fila.
          </p>
        </div>
        <button
          className="interruptor"
          role="switch"
          aria-checked={ativo}
          aria-labelledby="rotulo-som"
          onClick={() => {
            setAtivo(!ativo);
            definirSom(!ativo);
          }}
        />
      </li>
      {suportaNotificacoes() && (
        <li className="linha" style={{ flexWrap: 'wrap' }}>
          <div className="linha-corpo">
            <strong>Notificações do navegador</strong>
            <p className="secundario" style={{ fontSize: 14 }}>
              {permissao === 'granted'
                ? 'Ligadas: você é avisado mesmo com a aba em segundo plano.'
                : permissao === 'denied'
                  ? 'Bloqueadas no navegador. Libere nas configurações do site.'
                  : 'Seja avisado mesmo com a aba em segundo plano.'}
            </p>
          </div>
          {permissao === 'default' && (
            <button
              className="botao botao-suave botao-pequeno"
              onClick={async () => {
                const resultado = await pedirPermissao();
                setPermissao(resultado);
                if (resultado === 'granted') avisar('Notificações ligadas.');
              }}
            >
              Ligar
            </button>
          )}
        </li>
      )}
    </>
  );
}
