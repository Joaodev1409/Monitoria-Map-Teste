import { useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { MapPin, Send, Video } from 'lucide-react';
import { ErroApi } from '../api/cliente';
import { useAbrirSolicitacao, useTopicos } from '../api/consultas';
import type { Modalidade, NovaSolicitacao } from '../api/tipos';
import { Cabecalho, CaixaErro, Carregando } from '../componentes/Basicos';
import { useAvisos } from '../componentes/Avisos';

export function PedirAjuda() {
  const topicos = useTopicos();
  const abrir = useAbrirSolicitacao();
  const navegar = useNavigate();
  const avisar = useAvisos();
  const [parametros] = useSearchParams();
  const [dados, setDados] = useState<NovaSolicitacao>(() => ({
    topicoId: Number(parametros.get('topico')) || 0,
    titulo: '',
    descricao: '',
    modalidade: 'PRESENCIAL',
    linkReuniao: '',
  }));
  const [erros, setErros] = useState<Record<string, string>>({});

  const alterar = <K extends keyof NovaSolicitacao>(campo: K, valor: NovaSolicitacao[K]) => {
    setDados((atual) => ({ ...atual, [campo]: valor }));
    setErros((atual) => ({ ...atual, [campo]: '' }));
  };

  const enviar = (e: FormEvent) => {
    e.preventDefault();
    const locais: Record<string, string> = {};
    if (!dados.topicoId) locais.topicoId = 'Escolha o assunto da dúvida.';
    if (!dados.titulo.trim()) locais.titulo = 'Resuma a dúvida em uma frase.';
    if (!dados.descricao.trim()) locais.descricao = 'Conte um pouco do que aconteceu.';
    if (Object.keys(locais).length > 0) {
      setErros(locais);
      document.getElementById(Object.keys(locais)[0])?.focus();
      return;
    }
    abrir.mutate(dados, {
      onSuccess: (criada) => {
        avisar('Pedido enviado. Os monitores já foram avisados.');
        navegar(`/solicitacoes/${criada.id}`);
      },
      onError: (falha) => {
        if (falha instanceof ErroApi) setErros(falha.erros);
      },
    });
  };

  if (topicos.isLoading) return <Carregando linhas={2} />;

  return (
    <>
      <Cabecalho titulo="Pedir ajuda">
        Quanto mais contexto você der, mais rápido o monitor consegue te ajudar.
      </Cabecalho>

      <form className="cartao cartao-corpo formulario" onSubmit={enviar} noValidate style={{ maxWidth: 760 }}>
        <fieldset className="campo">
          <legend>Sobre o quê?</legend>
          <div className="pilulas" id="topicoId" tabIndex={-1} style={{ marginTop: 4 }}>
            {topicos.data?.map((t) => (
              <label key={t.id} className="pilula">
                <input
                  type="radio"
                  name="topico"
                  value={t.id}
                  checked={dados.topicoId === t.id}
                  onChange={() => alterar('topicoId', t.id)}
                />
                {t.nome}
              </label>
            ))}
          </div>
          {erros.topicoId && <span className="erro">{erros.topicoId}</span>}
        </fieldset>

        <div className="campo">
          <label htmlFor="titulo">Resuma em uma frase</label>
          <input
            id="titulo"
            className="entrada"
            maxLength={120}
            placeholder="Não entendi quando usar Observer em vez de Mediator"
            value={dados.titulo}
            onChange={(e) => alterar('titulo', e.target.value)}
            aria-invalid={Boolean(erros.titulo)}
            aria-describedby={erros.titulo ? 'erro-titulo' : undefined}
          />
          {erros.titulo && (
            <span id="erro-titulo" className="erro">
              {erros.titulo}
            </span>
          )}
        </div>

        <div className="campo">
          <label htmlFor="descricao">O que aconteceu?</label>
          <textarea
            id="descricao"
            className="entrada"
            maxLength={4000}
            placeholder="O que você tentou, o que esperava e o que aconteceu. Pode colar o trecho de código ou a mensagem de erro."
            value={dados.descricao}
            onChange={(e) => alterar('descricao', e.target.value)}
            aria-invalid={Boolean(erros.descricao)}
            aria-describedby={erros.descricao ? 'erro-descricao' : 'contador-descricao'}
          />
          {erros.descricao ? (
            <span id="erro-descricao" className="erro">
              {erros.descricao}
            </span>
          ) : (
            <span id="contador-descricao" className="contador-caracteres numero">
              {dados.descricao.length}/4000
            </span>
          )}
        </div>

        <fieldset className="campo">
          <legend>Como prefere ser atendido?</legend>
          <div className="opcoes" style={{ marginTop: 4 }}>
            {(
              [
                ['PRESENCIAL', MapPin, 'Presencial', 'No horário e local da monitoria'],
                ['ONLINE', Video, 'Online', 'Por chamada de vídeo'],
              ] as const
            ).map(([valor, Icone, titulo, texto]) => (
              <label key={valor} className="opcao">
                <input
                  type="radio"
                  name="modalidade"
                  value={valor}
                  checked={dados.modalidade === valor}
                  onChange={() => alterar('modalidade', valor as Modalidade)}
                />
                <span className="opcao-icone" aria-hidden>
                  <Icone size={20} />
                </span>
                <span>
                  <strong>{titulo}</strong>
                  <span className="secundario">{texto}</span>
                </span>
              </label>
            ))}
          </div>
        </fieldset>

        {dados.modalidade === 'ONLINE' && (
          <div className="campo">
            <label htmlFor="link">
              Link da reunião <span className="secundario">(opcional)</span>
            </label>
            <input
              id="link"
              className="entrada"
              type="url"
              inputMode="url"
              placeholder="https://meet.google.com/abc-defg-hij"
              value={dados.linkReuniao}
              onChange={(e) => alterar('linkReuniao', e.target.value)}
              aria-invalid={Boolean(erros.linkReuniao)}
              aria-describedby="dica-link"
            />
            <span id="dica-link" className={erros.linkReuniao ? 'erro' : 'dica'}>
              {erros.linkReuniao || 'Se deixar em branco, o monitor manda um link quando assumir.'}
            </span>
          </div>
        )}

        {abrir.isError && !(abrir.error instanceof ErroApi && Object.keys(abrir.error.erros).length > 0) && (
          <CaixaErro erro={abrir.error} />
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button type="submit" className="botao botao-acao botao-grande" aria-busy={abrir.isPending}>
            <Send size={18} aria-hidden />
            {abrir.isPending ? 'Enviando…' : 'Enviar pedido'}
          </button>
        </div>
      </form>
    </>
  );
}
