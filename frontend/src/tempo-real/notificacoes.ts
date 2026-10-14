// Notificações do navegador + um "bip" curto para os monitores perceberem pedidos novos.

const CHAVE_SOM = 'monitoria.som';

export const somAtivado = (): boolean => {
  try {
    return localStorage.getItem(CHAVE_SOM) !== 'nao';
  } catch {
    return true;
  }
};

export const definirSom = (ativo: boolean) => {
  try {
    localStorage.setItem(CHAVE_SOM, ativo ? 'sim' : 'nao');
  } catch {
    /* ignorado */
  }
};

export const suportaNotificacoes = () => typeof window !== 'undefined' && 'Notification' in window;

export async function pedirPermissao(): Promise<NotificationPermission> {
  if (!suportaNotificacoes()) return 'denied';
  if (Notification.permission !== 'default') return Notification.permission;
  return Notification.requestPermission();
}

/** Mostra a notificação só quando a aba não está em foco (na aba aberta o aviso na tela basta). */
export function notificarNavegador(titulo: string, corpo: string, comSom = false) {
  if (comSom && somAtivado()) tocarBip();
  if (!suportaNotificacoes() || Notification.permission !== 'granted' || document.visibilityState === 'visible') return;
  try {
    new Notification(titulo, { body: corpo, icon: '/favicon.svg', tag: 'monitoria-map' });
  } catch {
    /* alguns navegadores móveis só permitem via service worker */
  }
}

let audio: AudioContext | null = null;

function tocarBip() {
  try {
    audio ??= new AudioContext();
    const agora = audio.currentTime;
    [880, 1175].forEach((frequencia, i) => {
      const oscilador = audio!.createOscillator();
      const volume = audio!.createGain();
      oscilador.frequency.value = frequencia;
      volume.gain.setValueAtTime(0.0001, agora + i * 0.14);
      volume.gain.exponentialRampToValueAtTime(0.12, agora + i * 0.14 + 0.02);
      volume.gain.exponentialRampToValueAtTime(0.0001, agora + i * 0.14 + 0.13);
      oscilador.connect(volume).connect(audio!.destination);
      oscilador.start(agora + i * 0.14);
      oscilador.stop(agora + i * 0.14 + 0.14);
    });
  } catch {
    /* sem áudio disponível */
  }
}
