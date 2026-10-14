import type { Modalidade, StatusSolicitacao, TipoEvento } from '../api/tipos';

export const rotuloStatus: Record<StatusSolicitacao, string> = {
  ABERTA: 'Aberta',
  EM_ATENDIMENTO: 'Em atendimento',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
};

export const rotuloModalidade: Record<Modalidade, string> = {
  PRESENCIAL: 'Presencial',
  ONLINE: 'Online',
};

export const rotuloEvento: Record<TipoEvento, string> = {
  CRIADA: 'abriu a solicitação',
  ASSUMIDA: 'assumiu o atendimento',
  DEVOLVIDA: 'devolveu para a fila',
  CONCLUIDA: 'concluiu o atendimento',
  CANCELADA: 'cancelou a solicitação',
  LINK_DEFINIDO: 'enviou o link da reunião',
};

const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const relativo = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' });

export const formatarDataHora = (iso: string) => dataHora.format(new Date(iso));

/** "há 3 min", "há 2 h", "ontem"... */
export function tempoRelativo(iso: string, agora = Date.now()): string {
  const segundos = Math.round((new Date(iso).getTime() - agora) / 1000);
  const abs = Math.abs(segundos);
  if (abs < 45) return 'agora';
  if (abs < 3600) return relativo.format(Math.round(segundos / 60), 'minute');
  if (abs < 86400) return relativo.format(Math.round(segundos / 3600), 'hour');
  return relativo.format(Math.round(segundos / 86400), 'day');
}

/** Espera curta e legível: "agora mesmo", "há 4 min", "há 1 h 10 min". */
export function haQuanto(iso: string, agora = Date.now()): string {
  const minutos = Math.floor((agora - new Date(iso).getTime()) / 60_000);
  if (minutos < 1) return 'agora mesmo';
  if (minutos >= 24 * 60) return tempoRelativo(iso, agora);
  return `há ${duracao(minutos)}`;
}

/** 75 → "1 h 15 min" */
export function duracao(minutos: number | null | undefined): string {
  if (minutos == null) return '—';
  if (minutos < 1) return '< 1 min';
  const h = Math.floor(minutos / 60);
  const m = Math.round(minutos % 60);
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

export function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/);
  const primeira = partes[0]?.[0] ?? '';
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
  return (primeira + ultima).toUpperCase();
}

export const primeiroNome = (nome: string) => nome.trim().split(/\s+/)[0];

/** Data local no formato AAAA-MM-DD (para inputs type="date"). */
export function dataIso(data: Date): string {
  const ajustada = new Date(data.getTime() - data.getTimezoneOffset() * 60_000);
  return ajustada.toISOString().slice(0, 10);
}
