import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { api } from '../api/cliente';
import type { ConfiguracaoLogin } from '../api/tipos';
import { useAutenticacao } from '../auth/Autenticacao';
import { SimboloMarca } from '../componentes/Marca';

/**
 * Política de privacidade (pública). O Google pede este endereço para publicar o login,
 * e ela também serve aos alunos: diz em linguagem simples o que é guardado e por quê.
 */
export function Privacidade() {
  const { usuario } = useAutenticacao();
  const config = useQuery({
    queryKey: ['config-login'],
    queryFn: () => api<ConfiguracaoLogin>('/api/auth/config'),
    staleTime: Infinity,
  });
  const contato = config.data?.contatoEmail;

  return (
    <main className="conteudo" style={{ maxWidth: 760 }}>
      <Link to="/" className="botao botao-fantasma botao-pequeno" style={{ marginLeft: -10, marginBottom: 18 }}>
        <ArrowLeft size={16} aria-hidden />
        {usuario ? 'Voltar ao sistema' : 'Voltar ao login'}
      </Link>

      <article className="cartao cartao-corpo texto-longo">
        <div className="marca" style={{ marginBottom: 22 }}>
          <SimboloMarca />
          Monitoria MAP
        </div>
        <h1>Política de privacidade</h1>
        <p className="secundario">Última atualização: 16 de outubro de 2026.</p>

        <p>
          O Monitoria MAP é o sistema de pedidos de ajuda da monitoria de Métodos Avançados em Programação (MAP) da
          UEPB, mantido pelos monitores da disciplina. Ele não é um sistema oficial da universidade.
        </p>

        <Secao titulo="O que guardamos">
          <ul>
            <li>
              <strong>Da sua conta Google da UEPB:</strong> nome, e-mail institucional e foto de perfil. Não recebemos
              nem guardamos a sua senha.
            </li>
            <li>
              <strong>Dos seus pedidos de ajuda:</strong> título, descrição, assunto, modalidade, link da reunião (se
              houver), observações do monitor e o histórico de quando cada etapa aconteceu.
            </li>
            <li>
              <strong>Da conversa do pedido:</strong> as mensagens trocadas entre você e o monitor que assumiu o
              pedido, com data e hora.
            </li>
            <li>
              <strong>Seu papel no sistema:</strong> aluno, monitor ou administrador.
            </li>
          </ul>
        </Secao>

        <Secao titulo="Para que usamos">
          <ul>
            <li>Identificar quem pediu ajuda e quem atendeu.</li>
            <li>Avisar os monitores de pedidos novos e avisar você quando alguém assumir o seu pedido.</li>
            <li>Gerar números da monitoria (quantidade de atendimentos, tempo de espera, assuntos mais pedidos) para
              o relatório da monitoria.</li>
          </ul>
          <p>Não vendemos, não compartilhamos para publicidade e não usamos seus dados para nada além disso.</p>
        </Secao>

        <Secao titulo="Quem vê o quê">
          <ul>
            <li>Os outros alunos não veem os seus pedidos.</li>
            <li>Os monitores veem o seu nome, foto e os pedidos que você abriu, para poder atender.</li>
            <li>
              A conversa de um pedido é vista só por você, pelo monitor responsável e pela coordenação da
              monitoria, que pode ler (mas não escrever) em caso de problema. Os outros monitores não a veem.
            </li>
            <li>O seu e-mail só aparece para você e para a coordenação da monitoria (administradores).</li>
          </ul>
        </Secao>

        <Secao titulo="Cookies">
          <p>
            Usamos apenas dois cookies, ambos necessários para o sistema funcionar: um que mantém você conectado
            (por até 8 horas) e outro de proteção contra envios forjados por outros sites. Não usamos cookies de
            rastreamento nem de anúncios.
          </p>
        </Secao>

        <Secao titulo="Por quanto tempo">
          <p>
            Os dados ficam guardados enquanto a monitoria usar o sistema, para manter o histórico dos atendimentos.
            Você pode pedir a exclusão da sua conta e dos seus pedidos a qualquer momento.
          </p>
        </Secao>

        <Secao titulo="Seus direitos">
          <p>
            Pela Lei Geral de Proteção de Dados (LGPD), você pode pedir para ver, corrigir ou apagar os seus dados.
            {contato ? (
              <>
                {' '}
                Escreva para <a href={`mailto:${contato}`}>{contato}</a>.
              </>
            ) : (
              ' Fale com a coordenação da monitoria de MAP.'
            )}
          </p>
        </Secao>
      </article>
    </main>
  );
}

function Secao({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section>
      <h2>{titulo}</h2>
      {children}
    </section>
  );
}
