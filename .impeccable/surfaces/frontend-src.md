---
version: 1
slug: "frontend-src"
primary_target: "frontend/src"
related_targets: []
---

## Scope

App inteiro do Monitoria MAP (React, `frontend/src`): login, aluno (início, pedir ajuda, detalhe), monitor (fila, atendimentos), admin (relatórios, pessoas, tópicos). Modo: Operate. Celular e notebook com o mesmo peso.

## Direction contract

THESIS: Pedir e dar ajuda como um app de bolso acolhedor; recusa o painel SaaS cinza com barra lateral, selos coloridos e cartões iguais.
OWN-WORLD: Fundo de papel morno #F3EEE6, cartões brancos de canto 20px, tinta #1E1B18; verde-água #0F9D84 só no que se toca (botões, aba ativa, links). Status em tons de papel, sem saturação. Atkinson Hyperlegible Next e Mono.
STORY: O aluno vê em que etapa está e quem vai ajudar; o monitor vê quem espera há mais tempo e assume num toque.
FIRST VIEWPORT: Fila: saudação, contagem grande de quem espera, o mais antigo e o botão Assumir em destaque; aluno: cartão de etapas ao vivo ou o convite para pedir ajuda. No celular, barra inferior com o botão central de pedir.
FORM: digital-design-canon-warm-consumer-app-surface (alternativa escolhida na rodada 1), seed da12ff8e.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

Signature interaction: a etapa do pedido avança ao vivo (SSE) com mola suave; o cartão assumido desliza da fila para os atendimentos.
