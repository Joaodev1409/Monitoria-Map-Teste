import { useEffect, useLayoutEffect, useRef, useState, type FormEvent, type KeyboardEvent, type ReactNode } from 'react';
import { MessagesSquare, SendHorizontal } from 'lucide-react';
import { ErroApi } from '../api/cliente';
import { useEnviarMensagem, useMensagens } from '../api/consultas';
import type { Mensagem, Solicitacao, Usuario } from '../api/tipos';
import { formatarDataHora, primeiroNome } from '../util/formatar';
import { Avatar } from './Basicos';
import { linkSeguro } from './LinkReuniao';

const LIMITE = 4000;
const AVISAR_LIMITE_EM = 3500;
/** Mensagens do mesmo autor com menos que isso de intervalo ficam no mesmo bloco. */
const JUNTAR_ATE_MS = 5 * 60_000;

/** Espelha a regra do servidor: o aluno, o monitor que está com o pedido e o admin leem. */
export function podeLerConversa(s: Solicitacao, usuario: Usuario) {
  return s.aluno.id === usuario.id || s.monitor?.id === usuario.id || usuario.papel === 'ADMIN';
}

/** Já houve atendimento (e, portanto, pode existir conversa) neste pedido? */
const jaFoiAtendida = (s: Solicitacao) => s.status !== 'ABERTA' || s.historico.some((e) => e.tipo === 'ASSUMIDA');

/**
 * Conversa entre o aluno e o monitor responsável, dentro do pedido. Escrever só durante
 * o atendimento (regra do State no servidor); depois ela fica como registro, só para leitura.
 */
export function Conversa({ solicitacao: s, usuario }: { solicitacao: Solicitacao; usuario: Usuario }) {
  const pode = podeLerConversa(s, usuario) && jaFoiAtendida(s);
  const consulta = useMensagens(s.id, pode);
  const mensagens = consulta.data ?? [];

  const emAtendimento = s.status === 'EM_ATENDIMENTO';
  const participa = s.aluno.id === usuario.id || s.monitor?.id === usuario.id;
  if (!pode || (!emAtendimento && mensagens.length === 0)) return null;

  const outro = s.aluno.id === usuario.id ? s.monitor : s.aluno;

  return (
    <section className="cartao cartao-corpo conversa" aria-labelledby="conversa-titulo">
      <h2 id="conversa-titulo">Conversa</h2>
      <p className="secundario" style={{ marginTop: 2, fontSize: 15 }}>
        {participa && outro
          ? `Só você e ${primeiroNome(outro.nome)} veem estas mensagens.`
          : 'Entre o aluno e o monitor responsável.'}
      </p>

      {consulta.isError ? (
        <p className="erro" role="alert" style={{ marginTop: 14 }}>
          Não foi possível carregar a conversa. Recarregue a página.
        </p>
      ) : (
        <ListaDeMensagens mensagens={mensagens} usuario={usuario} carregando={consulta.isLoading} />
      )}

      {emAtendimento && participa ? (
        <Compositor solicitacaoId={s.id} />
      ) : (
        <p className="conversa-nota">
          {emAtendimento
            ? 'Você está acompanhando como administrador. Só o aluno e o monitor escrevem aqui.'
            : s.status === 'ABERTA'
              ? 'O pedido voltou para a fila. A conversa continua quando um monitor assumir.'
              : 'A conversa foi encerrada com o atendimento.'}
        </p>
      )}
    </section>
  );
}

function ListaDeMensagens({
  mensagens,
  usuario,
  carregando,
}: {
  mensagens: Mensagem[];
  usuario: Usuario;
  carregando: boolean;
}) {
  const lista = useRef<HTMLDivElement>(null);
  const noFim = useRef(true);
  const ultima = mensagens[mensagens.length - 1];

  // Desce sozinho quando chega mensagem, desde que a pessoa já estivesse no fim
  // (ou a mensagem seja dela). Se ela rolou para cima para reler, não puxamos a tela.
  useLayoutEffect(() => {
    const el = lista.current;
    if (!el) return;
    if (noFim.current || ultima?.autor.id === usuario.id) el.scrollTop = el.scrollHeight;
  }, [ultima?.id, usuario.id]); // eslint-disable-line react-hooks/exhaustive-deps

  if (carregando) {
    return (
      <div className="mensagens" aria-busy="true">
        <span className="esqueleto" style={{ height: 44, width: '55%', borderRadius: 16 }} />
        <span className="esqueleto" style={{ height: 44, width: '40%', borderRadius: 16, alignSelf: 'flex-end' }} />
      </div>
    );
  }

  return (
    <div
      ref={lista}
      className="mensagens"
      role="log"
      aria-live="polite"
      aria-label="Mensagens"
      tabIndex={0}
      onScroll={(e) => {
        const el = e.currentTarget;
        noFim.current = el.scrollHeight - el.scrollTop - el.clientHeight < 48;
      }}
    >
      {mensagens.length === 0 ? (
        <p className="conversa-vazia">
          <MessagesSquare size={22} aria-hidden />
          Nenhuma mensagem ainda. Mande um oi para combinar o atendimento.
        </p>
      ) : (
        agrupar(mensagens).map((grupo) => {
          const minha = grupo[0].autor.id === usuario.id;
          return (
            <div key={grupo[0].id} className={`grupo-mensagens ${minha ? 'minhas' : ''}`}>
              {!minha && <Avatar nome={grupo[0].autor.nome} foto={grupo[0].autor.fotoUrl} tamanho={32} />}
              <div className="grupo-corpo">
                <p className="grupo-autor">
                  {minha ? 'Você' : primeiroNome(grupo[0].autor.nome)}
                  <time dateTime={grupo[0].enviadaEm} title={formatarDataHora(grupo[0].enviadaEm)}>
                    {horario(grupo[0].enviadaEm)}
                  </time>
                </p>
                {grupo.map((m) => (
                  <div key={m.id} className="mensagem">
                    <TextoDaMensagem texto={m.texto} />
                  </div>
                ))}
              </div>
            </div>
          );
        })
      )}
    </div>
  );
}

function Compositor({ solicitacaoId }: { solicitacaoId: number }) {
  const enviarMensagem = useEnviarMensagem();
  const [texto, setTexto] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const campo = useRef<HTMLTextAreaElement>(null);
  const tamanho = texto.length;

  // O campo cresce com o texto até umas 6 linhas; dali em diante rola por dentro.
  useEffect(() => {
    const el = campo.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 180)}px`;
  }, [texto]);

  const enviar = (e?: FormEvent) => {
    e?.preventDefault();
    if (!texto.trim() || enviarMensagem.isPending) return;
    if (tamanho > LIMITE) {
      setErro(`A mensagem pode ter até ${LIMITE} caracteres.`);
      return;
    }
    setErro(null);
    enviarMensagem.mutate(
      { solicitacaoId, texto },
      {
        onSuccess: () => {
          setTexto('');
          campo.current?.focus();
        },
        onError: (falha) => setErro(falha instanceof ErroApi ? falha.erros.texto ?? falha.message : falha.message),
      },
    );
  };

  // Enter envia e Shift+Enter quebra a linha. No celular (toque), Enter só quebra a linha
  // e quem envia é o botão, como nos aplicativos de mensagem.
  const aoTeclar = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key !== 'Enter' || e.shiftKey || e.nativeEvent.isComposing) return;
    if (window.matchMedia('(pointer: coarse)').matches) return;
    e.preventDefault();
    enviar();
  };

  return (
    <form className="compositor" onSubmit={enviar} noValidate>
      <label htmlFor="nova-mensagem" className="sr-only">
        Escrever mensagem
      </label>
      <div className="compositor-linha">
        <textarea
          id="nova-mensagem"
          ref={campo}
          className="entrada"
          rows={1}
          placeholder="Escreva uma mensagem…"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          onKeyDown={aoTeclar}
          aria-invalid={erro ? true : undefined}
          aria-describedby="dica-mensagem"
        />
        <button
          type="submit"
          className="botao botao-acao"
          disabled={!texto.trim()}
          aria-busy={enviarMensagem.isPending || undefined}
        >
          <SendHorizontal size={18} aria-hidden />
          <span className="rotulo-enviar">{enviarMensagem.isPending ? 'Enviando…' : 'Enviar'}</span>
        </button>
      </div>
      <p id="dica-mensagem" className={erro ? 'erro' : 'dica'}>
        {erro ??
          (tamanho >= AVISAR_LIMITE_EM
            ? `${tamanho} de ${LIMITE} caracteres.`
            : 'Para mandar código, coloque entre ``` (três crases).')}
      </p>
    </form>
  );
}

/** Junta mensagens seguidas do mesmo autor (com pouco intervalo) num bloco só. */
function agrupar(mensagens: Mensagem[]): Mensagem[][] {
  const grupos: Mensagem[][] = [];
  for (const m of mensagens) {
    const atual = grupos[grupos.length - 1];
    const anterior = atual?.[atual.length - 1];
    const junto =
      anterior &&
      anterior.autor.id === m.autor.id &&
      new Date(m.enviadaEm).getTime() - new Date(anterior.enviadaEm).getTime() < JUNTAR_ATE_MS;
    if (junto) atual.push(m);
    else grupos.push([m]);
  }
  return grupos;
}

const soHora = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });

/** "14:51" se foi hoje; data e hora curtas se foi outro dia. */
function horario(iso: string) {
  const data = new Date(iso);
  return data.toDateString() === new Date().toDateString() ? soHora.format(data) : formatarDataHora(iso);
}

/**
 * Formata o texto como nós do React (nunca HTML cru, então não há como injetar script):
 * ```bloco``` vira código em bloco, `assim` vira código na linha e links http(s) ficam clicáveis.
 */
function TextoDaMensagem({ texto }: { texto: string }) {
  const partes: ReactNode[] = [];
  // O nome da linguagem (```java) só conta se vier sozinho na primeira linha. Sem essa
  // exigência, ```notificar()``` numa linha só virava "linguagem notificar()" e um bloco vazio.
  const blocos = /```(?:[a-zA-Z0-9+#._-]*\n)?([\s\S]*?)```/g;
  let inicio = 0;
  for (const achado of texto.matchAll(blocos)) {
    const posicao = achado.index ?? 0;
    if (posicao > inicio) partes.push(...emLinha(texto.slice(inicio, posicao), partes.length));
    const codigo = achado[1].replace(/\n$/, '');
    if (codigo.trim()) {
      partes.push(
        <pre key={`b${partes.length}`}>
          <code>{codigo}</code>
        </pre>,
      );
    }
    inicio = posicao + achado[0].length;
  }
  if (inicio < texto.length) partes.push(...emLinha(texto.slice(inicio), partes.length));
  // Só crases, sem nada dentro: mostra o que foi digitado em vez de um balão vazio.
  return partes.length > 0 ? <>{partes}</> : <p>{texto}</p>;
}

function emLinha(trecho: string, base: number): ReactNode[] {
  const texto = trecho.replace(/^\n+|\n+$/g, '');
  if (!texto) return [];
  // O link não leva a pontuação que costuma vir colada no fim da frase ("veja https://x.com.").
  const pedacos = texto.split(/(`[^`\n]+`|https?:\/\/[^\s<>()]*[^\s<>().,;:!?'"])/g);
  return [
    <p key={`t${base}`}>
      {pedacos.map((pedaco, i) => {
        if (pedaco.startsWith('`') && pedaco.endsWith('`') && pedaco.length > 2) {
          return <code key={i}>{pedaco.slice(1, -1)}</code>;
        }
        const link = linkSeguro(pedaco);
        if (link) {
          return (
            <a key={i} href={link} target="_blank" rel="noopener noreferrer">
              {pedaco}
            </a>
          );
        }
        return pedaco;
      })}
    </p>,
  ];
}
