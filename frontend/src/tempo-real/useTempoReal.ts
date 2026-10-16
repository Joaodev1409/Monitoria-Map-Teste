import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { conferirSessao } from '../api/cliente';
import { acrescentarMensagem, chaves } from '../api/consultas';
import type { EventoTempoReal, Mensagem, Usuario } from '../api/tipos';
import { podeAtender } from '../auth/Autenticacao';
import { primeiroNome } from '../util/formatar';
import { useAvisos } from '../componentes/Avisos';
import { notificarNavegador } from './notificacoes';

/**
 * Mantém uma conexão Server-Sent Events com /api/eventos/stream.
 *
 * Como a sessão é um cookie, o EventSource nativo do navegador basta (ele envia o cookie
 * sozinho). Ele já reconecta quando a rede cai; se o servidor recusar (ex.: reiniciando),
 * a conexão fecha e tentamos de novo com espera crescente (1s, 2s, 4s… até 30s).
 */
export function useTempoReal(usuario: Usuario | null): boolean {
  const cliente = useQueryClient();
  const avisar = useAvisos();
  const [conectado, setConectado] = useState(false);
  const usuarioRef = useRef(usuario);
  usuarioRef.current = usuario;
  const { pathname } = useLocation();
  const caminhoRef = useRef(pathname);
  caminhoRef.current = pathname;

  useEffect(() => {
    if (!usuario) return;
    let fonte: EventSource | null = null;
    let tentativas = 0;
    let espera: ReturnType<typeof setTimeout> | undefined;
    let ativo = true;

    const aoReceber = (mensagem: MessageEvent<string>) => {
      const { tipo, solicitacao } = JSON.parse(mensagem.data) as EventoTempoReal;
      cliente.setQueryData(chaves.detalhe(solicitacao.id), solicitacao);
      cliente.invalidateQueries({ queryKey: chaves.solicitacoes });

      const eu = usuarioRef.current;
      if (!eu) return;
      const ehMinha = solicitacao.aluno.id === eu.id;
      if (ehMinha && tipo === 'ASSUMIDA' && solicitacao.monitor) {
        avisar(`${primeiroNome(solicitacao.monitor.nome)} assumiu o seu pedido.`, 'info');
        notificarNavegador('Um monitor vai te ajudar', `${solicitacao.monitor.nome} assumiu "${solicitacao.titulo}".`);
      } else if (ehMinha && tipo === 'CONCLUIDA') {
        avisar('Seu atendimento foi concluído.', 'info');
      } else if (podeAtender(eu.papel) && !ehMinha && (tipo === 'CRIADA' || tipo === 'DEVOLVIDA')) {
        const titulo = tipo === 'CRIADA' ? 'Pedido novo na fila' : 'Um pedido voltou para a fila';
        avisar(`${titulo}: ${solicitacao.titulo}`, 'info');
        notificarNavegador(titulo, `${solicitacao.aluno.nome} · ${solicitacao.topico.nome}`, true);
      }
    };

    // Mensagem nova na conversa: entra na tela na hora. Se a pessoa estiver em outra página
    // (ou com a aba em segundo plano), também ganha um aviso e o bip.
    const aoReceberMensagem = (evento: MessageEvent<string>) => {
      const mensagem = JSON.parse(evento.data) as Mensagem;
      cliente.setQueryData<Mensagem[]>(chaves.mensagens(mensagem.solicitacaoId), (lista) =>
        acrescentarMensagem(lista, mensagem),
      );

      const eu = usuarioRef.current;
      if (!eu || mensagem.autor.id === eu.id) return;
      const vendoAConversa =
        document.visibilityState === 'visible' && caminhoRef.current === `/solicitacoes/${mensagem.solicitacaoId}`;
      if (vendoAConversa) return;
      const quem = primeiroNome(mensagem.autor.nome);
      const trecho = mensagem.texto.replace(/\s+/g, ' ').slice(0, 80);
      avisar(`${quem}: ${trecho}`, 'info');
      notificarNavegador(`Mensagem de ${quem}`, trecho, true);
    };

    const conectar = () => {
      fonte = new EventSource('/api/eventos/stream');
      fonte.onopen = () => {
        setConectado(true);
        tentativas = 0;
        // Recarrega o que pode ter mudado enquanto estávamos desconectados.
        cliente.invalidateQueries({ queryKey: chaves.solicitacoes });
        cliente.invalidateQueries({ queryKey: ['mensagens'] });
      };
      fonte.addEventListener('solicitacao', aoReceber as EventListener);
      fonte.addEventListener('mensagem', aoReceberMensagem as EventListener);
      fonte.onerror = () => {
        setConectado(false);
        if (fonte?.readyState === EventSource.CLOSED && ativo) {
          fonte.close();
          // A conexão pode ter sido recusada porque a sessão acabou (ex.: "Sair" em outro aparelho).
          void conferirSessao();
          espera = setTimeout(conectar, Math.min(30_000, 1000 * 2 ** tentativas++));
        }
      };
    };

    conectar();
    return () => {
      ativo = false;
      clearTimeout(espera);
      fonte?.close();
    };
  }, [usuario?.id, cliente, avisar]); // eslint-disable-line react-hooks/exhaustive-deps

  return conectado;
}
