// Ganchos do TanStack Query: cada tela pede os dados por aqui e o cache é compartilhado.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, enviar } from './cliente';
import type {
  Mensagem,
  NovaSolicitacao,
  Papel,
  ResumoRelatorio,
  Solicitacao,
  StatusSolicitacao,
  Topico,
  Usuario,
} from './tipos';

export const chaves = {
  solicitacoes: ['solicitacoes'] as const,
  minhas: ['solicitacoes', 'minhas'] as const,
  fila: (status: StatusSolicitacao) => ['solicitacoes', 'status', status] as const,
  atendimentos: ['solicitacoes', 'atendimentos'] as const,
  detalhe: (id: number) => ['solicitacoes', 'detalhe', id] as const,
  // Fora de 'solicitacoes' de propósito: mudar a fila não precisa recarregar as conversas.
  mensagens: (id: number) => ['mensagens', id] as const,
  topicos: (todos: boolean) => ['topicos', todos] as const,
  usuarios: ['usuarios'] as const,
  relatorio: (de: string, ate: string) => ['relatorio', de, ate] as const,
};

export const useMinhasSolicitacoes = () =>
  useQuery({ queryKey: chaves.minhas, queryFn: () => api<Solicitacao[]>('/api/solicitacoes/minhas') });

export const useFila = (status: StatusSolicitacao = 'ABERTA', habilitado = true) =>
  useQuery({
    queryKey: chaves.fila(status),
    queryFn: () => api<Solicitacao[]>(`/api/solicitacoes?status=${status}`),
    enabled: habilitado,
  });

export const useMeusAtendimentos = () =>
  useQuery({ queryKey: chaves.atendimentos, queryFn: () => api<Solicitacao[]>('/api/solicitacoes/atendimentos') });

export const useSolicitacao = (id: number) =>
  useQuery({ queryKey: chaves.detalhe(id), queryFn: () => api<Solicitacao>(`/api/solicitacoes/${id}`) });

export const useMensagens = (solicitacaoId: number, habilitado: boolean) =>
  useQuery({
    queryKey: chaves.mensagens(solicitacaoId),
    queryFn: () => api<Mensagem[]>(`/api/solicitacoes/${solicitacaoId}/mensagens`),
    enabled: habilitado,
  });

/** Acrescenta uma mensagem à conversa em cache sem duplicar (ela chega pela resposta e pelo canal ao vivo). */
export function acrescentarMensagem(lista: Mensagem[] | undefined, nova: Mensagem): Mensagem[] | undefined {
  if (!lista) return lista;
  return lista.some((m) => m.id === nova.id) ? lista : [...lista, nova];
}

export function useEnviarMensagem() {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: ({ solicitacaoId, texto }: { solicitacaoId: number; texto: string }) =>
      enviar<Mensagem>(`/api/solicitacoes/${solicitacaoId}/mensagens`, 'POST', { texto }),
    onSuccess: (mensagem) =>
      cliente.setQueryData<Mensagem[]>(chaves.mensagens(mensagem.solicitacaoId), (lista) =>
        acrescentarMensagem(lista, mensagem),
      ),
  });
}

/** "Outro assunto" (ou qualquer tópico que comece com "Outro") vai sempre para o fim da lista. */
const outroPorUltimo = (a: Topico, b: Topico) =>
  Number(/^outr/i.test(a.nome)) - Number(/^outr/i.test(b.nome)) || a.nome.localeCompare(b.nome, 'pt-BR');

export const useTopicos = (incluirInativos = false) =>
  useQuery({
    queryKey: chaves.topicos(incluirInativos),
    queryFn: async () => (await api<Topico[]>(`/api/topicos?incluirInativos=${incluirInativos}`)).sort(outroPorUltimo),
    staleTime: 5 * 60_000,
  });

export const useUsuarios = () => useQuery({ queryKey: chaves.usuarios, queryFn: () => api<Usuario[]>('/api/usuarios') });

export const useRelatorio = (de: string, ate: string) =>
  useQuery({
    queryKey: chaves.relatorio(de, ate),
    queryFn: () => api<ResumoRelatorio>(`/api/relatorios/resumo?de=${de}&ate=${ate}`),
    enabled: Boolean(de && ate),
  });

/** Depois de qualquer mudança, recarrega todas as listas de solicitações. */
function useAoAlterarSolicitacao() {
  const cliente = useQueryClient();
  return (solicitacao: Solicitacao) => {
    cliente.setQueryData(chaves.detalhe(solicitacao.id), solicitacao);
    cliente.invalidateQueries({ queryKey: chaves.solicitacoes });
    cliente.invalidateQueries({ queryKey: ['relatorio'] });
  };
}

export function useAbrirSolicitacao() {
  const aoAlterar = useAoAlterarSolicitacao();
  return useMutation({
    mutationFn: (dados: NovaSolicitacao) => enviar<Solicitacao>('/api/solicitacoes', 'POST', dados),
    onSuccess: aoAlterar,
  });
}

export type AcaoSolicitacao = 'assumir' | 'concluir' | 'devolver' | 'cancelar';

export function useAcaoSolicitacao() {
  const aoAlterar = useAoAlterarSolicitacao();
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: ({ id, acao, observacao }: { id: number; acao: AcaoSolicitacao; observacao?: string }) =>
      enviar<Solicitacao>(
        `/api/solicitacoes/${id}/${acao}`,
        'POST',
        acao === 'concluir' ? { observacao: observacao ?? '' } : undefined,
      ),
    onSuccess: aoAlterar,
    onError: () => {
      // Ex.: outro monitor assumiu primeiro. Recarrega para mostrar o estado real.
      cliente.invalidateQueries({ queryKey: chaves.solicitacoes });
    },
  });
}

export function useDefinirLink() {
  const aoAlterar = useAoAlterarSolicitacao();
  return useMutation({
    mutationFn: ({ id, linkReuniao }: { id: number; linkReuniao: string }) =>
      enviar<Solicitacao>(`/api/solicitacoes/${id}/link`, 'PATCH', { linkReuniao }),
    onSuccess: aoAlterar,
  });
}

export function useAlterarUsuario() {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: ({ id, papel, ativo }: { id: number; papel?: Papel; ativo?: boolean }) =>
      papel
        ? enviar<Usuario>(`/api/usuarios/${id}/papel`, 'PATCH', { papel })
        : enviar<Usuario>(`/api/usuarios/${id}/ativo`, 'PATCH', { ativo }),
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.usuarios }),
  });
}

export function useSalvarTopico() {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: ({ id, nome, ativo }: { id?: number; nome?: string; ativo?: boolean }) =>
      id ? enviar<Topico>(`/api/topicos/${id}`, 'PATCH', { nome, ativo }) : enviar<Topico>('/api/topicos', 'POST', { nome }),
    onSuccess: () => cliente.invalidateQueries({ queryKey: ['topicos'] }),
  });
}
