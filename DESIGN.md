# Design · Monitoria MAP

Registro do sistema visual como está construído em `frontend/src` (fonte da verdade: `frontend/src/estilos.css`).
Direção: **app de bolso acolhedor**. Papel morno, cartões brancos de canto macio, rótulos escritos como gente
fala, e **uma única cor viva, reservada para o que se pode tocar**.

## Cor

| Papel | Claro | Escuro | Uso |
|---|---|---|---|
| `--fundo` | `#f3eee6` | `#171411` | Fundo da página (papel morno) |
| `--fundo-fundo` | `#ebe4d8` | `#120f0d` | Trilhas, segmentos, avatar sem foto, hover |
| `--cartao` | `#ffffff` | `#221e1a` | Cartões |
| `--cartao-2` | `#faf7f2` | `#1c1916` | Campos de formulário, pílulas |
| `--borda` / `--borda-forte` | `#e6ddcf` / `#d3c7b4` | `#332d27` / `#4a4138` | Divisórias, contornos |
| `--tinta` / `-2` / `-3` | `#1e1b18` / `#665d55` / `#7a7067` | `#f3ede5` / `#b9aea3` / `#988d82` | Texto principal, secundário, dica |
| `--acao` | `#0b8571` | `#3cc3ac` | **Só** botões de ação, aba ativa, links, interruptor ligado, etapa concluída |
| `--acao-suave` / `--acao-texto` | `#dcf0ea` / `#0a6b5b` | `#16352f` / `#7fdccb` | Botão suave, aba ativa, seleção de texto |

**Status nunca usa a cor de ação.** Cada status é um tom de papel com ícone:
Aberta (areia `#f6e6c6`/`#74500f`, relógio) · Em atendimento (azul-acinzentado `#dbe6f1`/`#2b4c70`, fone) ·
Concluída (sálvia `#dbeadc`/`#2d5b33`, check) · Cancelada (`#ebe5dc`/`#625a52`, x).
Perigo: `#b42318` sobre `#fbe5e1`.

O verde de ação foi escurecido de `#0f9d84` para `#0b8571` para o texto branco nos botões passar de 4,5:1.

## Tipografia

- **Atkinson Hyperlegible Next** (variável, auto-hospedada via `@fontsource-variable`). Foi desenhada para
  legibilidade máxima, e é a única família de texto.
- **Atkinson Hyperlegible Mono** só para medidas e números tabulares quando necessário (não como "fantasia técnica").
- Escala: h1 `clamp(1.75rem → 2.35rem)` peso 800, `-0.025em`; h2 1.2rem; corpo 16px/1.55; meta 14px; rótulos 13px.
  Título do login até 3.75rem, peso 800, `-0.04em`.
- Tempos e contagens usam `font-variant-numeric: tabular-nums`.

## Forma e profundidade

- Raios: cartão 20px, controle 14px, pílula 999px, avatar ≈36% do lado (quadrado arredondado).
- Sombra de cartão quente e difusa (`0 10px 28px -14px rgb(70 48 24 / .22)`); flutuante para barra inferior e avisos.
- Listas são **linhas dentro de um único cartão**, com divisória recuada (nunca cartões soltos repetidos).

## Componentes

- **Botões**: `botao-acao` (verde, uma ação principal por área), `botao-suave` (verde claro, secundário),
  `botao-fantasma`, `botao-perigo` (contorno vermelho). Altura 44px (grande 54, pequeno 36).
- **Etapas** (`Etapas.tsx`): Na fila → Com {monitor} → Concluído. É a assinatura do app.
- **Pílulas** de tópico (rádio), **opções** de modalidade (cartão com ícone), **segmentos**, **interruptor**.
- **Confirmação em linha** para ações que desfazem algo (cancelar, devolver), sem `window.confirm`.
- **Avisos** (toasts) em tinta escura centralizados, acima da barra inferior no celular.
- **Marca**: SVG de uma fila de três pontos, com o primeiro aceso na cor de ação.

## Layout

- Notebook (≥900px): barra no topo com vidro leve, navegação em pílulas, indicador "Ao vivo" e avatar → Conta.
- Celular (<900px): barra inferior flutuante (raio 24px) ao alcance do polegar. O aluno tem um botão central
  elevado "Pedir ajuda"; o monitor e o admin têm Fila (com contador), Atendimentos, Gestão e Conta.
- Fila do monitor: coluna principal (título-frase, "próximo da fila" em destaque, "na sequência" em linhas) +
  coluna lateral de 340px ("Com você agora", "Seu dia").
- Título com selo: lado a lado; abaixo de 560px o selo sobe acima do título.

## Movimento

- `--saida: cubic-bezier(.16,1,.3,1)` (desaceleração exponencial) para entradas e progresso.
- `--mola: cubic-bezier(.3,1.25,.5,1)` só para retorno de toque (botões, pílulas, interruptor). Exceção registrada
  no detector, porque a direção pede "mola suave no toque".
- Momentos com autoria: a barra de etapas avança (scaleX) e a etapa atual pulsa; um pedido que chega ao vivo
  entra com um brilho verde; ao **assumir**, o cartão desliza da fila para "Com você agora" (View Transitions,
  `view-transition-name: pedido-{id}`).
- `prefers-reduced-motion`: tudo vira troca instantânea; as View Transitions são puladas.

## Regras

1. A cor de ação aparece só onde dá para tocar. Status, gráficos (barras em `--tinta-2`) e decoração ficam neutros.
2. Sem sobretítulo (kicker) acima de títulos; o título carrega o próprio peso. Contagens viram frases ("2 alunos esperando").
3. Sem cartões repetidos de ícone + título + texto; agrupe em linhas dentro de um cartão.
4. Texto de interface em português, direto, de colega para colega; botões dizem a ação ("Assumir", "Enviar pedido").
5. Superfícies do navegador também seguem o tema: seleção, cursor de texto, barra de rolagem, anel de foco, sublinhado.
