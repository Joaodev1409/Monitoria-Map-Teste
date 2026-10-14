// Espelho dos DTOs do back-end (pacotes *.dto e records dos controllers).

export type Papel = 'ALUNO' | 'MONITOR' | 'ADMIN';
export type StatusSolicitacao = 'ABERTA' | 'EM_ATENDIMENTO' | 'CONCLUIDA' | 'CANCELADA';
export type Modalidade = 'PRESENCIAL' | 'ONLINE';
export type TipoEvento = 'CRIADA' | 'ASSUMIDA' | 'DEVOLVIDA' | 'CONCLUIDA' | 'CANCELADA' | 'LINK_DEFINIDO';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  fotoUrl: string | null;
  papel: Papel;
  ativo: boolean;
  telefone: string | null;
  criadoEm: string;
}

/** O que se sabe de outra pessoa: nome e foto (sem e-mail, por privacidade). */
export interface UsuarioResumo {
  id: number;
  nome: string;
  fotoUrl: string | null;
}

export interface Topico {
  id: number;
  nome: string;
  ativo: boolean;
}

export interface EventoHistorico {
  tipo: TipoEvento;
  autor: UsuarioResumo;
  ocorridoEm: string;
  detalhe: string | null;
}

export interface Solicitacao {
  id: number;
  titulo: string;
  descricao: string;
  topico: { id: number; nome: string };
  modalidade: Modalidade;
  linkReuniao: string | null;
  status: StatusSolicitacao;
  aluno: UsuarioResumo;
  monitor: UsuarioResumo | null;
  criadaEm: string;
  assumidaEm: string | null;
  concluidaEm: string | null;
  canceladaEm: string | null;
  observacaoConclusao: string | null;
  historico: EventoHistorico[];
}

export interface NovaSolicitacao {
  topicoId: number;
  titulo: string;
  descricao: string;
  modalidade: Modalidade;
  linkReuniao: string;
}

/** O token não vem aqui: ele chega num cookie HttpOnly. */
export interface LoginResposta {
  usuario: Usuario;
  sessaoExpiraEm: string;
}

export interface ConfiguracaoLogin {
  googleClientId: string | null;
  dominiosPermitidos: string[];
}

export interface Contagem {
  rotulo: string;
  total: number;
}

export interface ResumoRelatorio {
  de: string;
  ate: string;
  total: number;
  porStatus: Record<StatusSolicitacao, number>;
  tempoMedioEsperaMinutos: number | null;
  tempoMedioAtendimentoMinutos: number | null;
  porMonitor: {
    monitorId: number;
    nome: string;
    atendimentosConcluidos: number;
    tempoMedioAtendimentoMinutos: number | null;
  }[];
  porTopico: Contagem[];
  porModalidade: Record<Modalidade, number>;
  porSemana: Contagem[];
}

export interface EventoTempoReal {
  tipo: TipoEvento;
  solicitacao: Solicitacao;
}
