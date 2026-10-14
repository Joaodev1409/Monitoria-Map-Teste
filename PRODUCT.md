# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Alunos** de Métodos Avançados em Programação (MAP) da UEPB, que travam em exercícios, listas ou conceitos (orientação a objetos, padrões de projeto GoF, testes, refatoração) e precisam pedir ajuda a um monitor.
- **Monitores** da disciplina (estudantes que já cursaram MAP), que recebem os pedidos, assumem um, atendem presencialmente ou online e marcam como concluído.
- **Coordenação/admin** (o monitor responsável e/ou o professor), que promove monitores, cuida dos tópicos e tira relatórios do semestre.

Celular e notebook são igualmente importantes. O monitor pode estar entre aulas no celular ou com o sistema aberto no notebook durante o horário de monitoria.

## Product Purpose

Substituir os pedidos de ajuda que se perdiam em grupos de mensagem por uma fila rastreável. O aluno pede, todos os monitores são avisados na hora, um assume e conclui. Fica registrado quando o aluno pediu, quem atendeu e quanto tempo levou. Sucesso significa nenhum aluno esquecido na fila, além de números confiáveis para o relatório de monitoria.

## Positioning

É um helpdesk feito para uma disciplina específica, por um monitor dela. O código também serve de material de aula: ele aplica os padrões que a própria disciplina ensina (State no ciclo de vida, Observer e Strategy nas notificações).

## Operating Context

- Horários de monitoria presenciais na UEPB e atendimentos online por link de reunião.
- Semestre letivo com listas de exercícios e projeto da disciplina.
- O relatório de monitoria é exportado em CSV.

## Capabilities and Constraints

- Ciclo da solicitação: ABERTA → EM_ATENDIMENTO → CONCLUÍDA; ABERTA → CANCELADA; EM_ATENDIMENTO → ABERTA (devolvida).
- Atualização em tempo real (SSE), avisos por e-mail, som e notificação do navegador para monitores.
- Login com Google, com restrição opcional ao domínio institucional. Papéis: ALUNO, MONITOR e ADMIN.
- Stack: Spring Boot (Java 17) e React + Vite + TypeScript. Deploy ainda indefinido (Docker; front na Vercel).
- Não é um sistema oficial da UEPB: não usar a logo nem a identidade institucional.
- WhatsApp fica para uma fase futura.

## Brand Commitments

- Nome: **Monitoria MAP**.
- Interface e textos em português do Brasil, em tom direto e acolhedor, de colega para colega.
- Sem logo nem cores oficiais da UEPB, por decisão do usuário.

## Evidence on Hand

Não há usuários reais, depoimentos nem métricas ainda. Nenhum número de uso pode ser inventado. Dados de exemplo (alunos, dúvidas) precisam ser claramente fictícios.

## Product Principles

1. Nenhum aluno esquecido: o tempo de espera é sempre visível, e quem espera há mais tempo vem primeiro.
2. A ação certa a um toque: assumir, concluir e pedir ajuda nunca ficam escondidos.
3. Estado sempre honesto: o que a tela mostra é o que está no servidor, ao vivo.
4. Privacidade de colega: cada pessoa vê só o que precisa ver.
5. O próprio produto ensina: as decisões de código são exemplos para a turma.

## Accessibility & Inclusion

Precisa funcionar bem no celular e com teclado, respeitar `prefers-reduced-motion` e manter contraste adequado nos modos claro e escuro.
