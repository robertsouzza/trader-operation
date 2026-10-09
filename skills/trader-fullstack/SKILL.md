---
name: trader-fullstack
description: Meta-skill/índice do Trader Operation (plataforma web de análise e operação em tempo real com IA, integrada ao MetaTrader 5). Guarda os documentos de referência (decisões, visão e requisitos, arquitetura) e define a ordem das skills numeradas trader-00 a trader-12. Use quando o usuário pedir para gerar ou continuar o Trader Operation, perguntar por onde começar, ou precisar consultar decisões já tomadas.
---

# Trader Operation — Meta-skill / Índice

## O que é

Plataforma web no estilo TradingView, ligada ao MetaTrader 5, com IA embutida. O operador master e a IA publicam operações (entrada, stop, alvos) em tempo real; clientes assinantes acompanham, conversam entre si e replicam no MT5 da própria máquina pelo **Copiloto IA**. Há uma vitrine de estratégias validadas por backtest. Visão completa em `references/visao-e-requisitos.md`.

## Documentos de referência (leia antes de qualquer skill)

1. `references/decisoes-tomadas.md` — decisões D-01 em diante. **Em caso de conflito, este arquivo vence.**
2. `references/visao-e-requisitos.md` — perfis, planos, RF/RNF, riscos.
3. `references/arquitetura.md` — partes do sistema, responsabilidades, módulos do backend.

Ambiguidade que esses três documentos não cobrem: **pare e pergunte**, não assuma. A resposta vira uma nova decisão em `decisoes-tomadas.md`.

## Roadmap de skills

Cada skill tem escopo pequeno, um bloco **Assume** com o que a anterior entrega e um **Definition of Done verificável por comando**. Só avance quando o DoD passar. O `SKILL.md` de cada skill é escrito e revisado com o usuário quando chegar a vez dela.

| # | Skill | Produz |
|---|---|---|
| 00 | `trader-00-infra-base` | Monorepo, `docker-compose.yml` (Postgres+pgvector, Redis, S3 via SeaweedFS), referências, `CLAUDE.md` |
| 01 | `trader-01-nucleo-core` | Spring Boot: `shared`, `autenticacao` (cookie JWT + CSRF), `auditoria`, ArchUnit |
| 02 | `trader-02-planos-perfis` | Perfis (admin, master, cliente), planos Free/Pro/Premium, matriz de direitos |
| 03 | `trader-03-frontend-core` | React: login por cookie, layout, rotas protegidas por perfil e plano |
| 04 | `trader-04-motor-quant-base` | FastAPI: dados de mercado, candles, indicadores |
| 05 | `trader-05-operacoes-tempo-real` | Operações publicadas, Redis + STOMP, contador de participantes, chat |
| 06 | `trader-06-frontend-grafico` | Lightweight Charts ao vivo, operações no gráfico, chat da operação |
| 07 | `trader-07-ia-analista` | Provedores de IA plugáveis, ferramentas da IA, chat com a IA |
| 08 | `trader-08-estrategias-backtest` | Vitrine, pedido de teste, backtest (vectorbt), métricas, aprovação do master |
| 09 | `trader-09-pagamentos` | Adapters de pagamento e assinatura recorrente |
| 10 | `trader-10-conector-mt5` | Fork do metatrader-mcp-server + trava de risco local + indicador MQL5 |
| 11 | `trader-11-copiloto` | App Electron do cliente: login, operações, chat com IA, aciona o conector |
| 12 | `trader-12-testes-cicd` | Testcontainers, Playwright, GitHub Actions, Dockerfiles de produção |

Dependência linear: cada skill assume as anteriores.

## Regras que valem para todas as skills

- **Idioma:** código em inglês; comentários, mensagens de erro, commits e docs em português (BR).
- **Nada de dados reais** em seed/fixture/teste: só dados sintéticos, com aviso no topo do arquivo. Nunca commitar chaves de API, senhas de MT5 ou `.env`.
- **Motor de risco é inegociável:** nenhuma ordem chega ao MT5 sem passar por ele; a IA não tem ferramenta para alterá-lo.
- **Estratégia não sai da nuvem** (D-03): nenhum endpoint devolve as regras de uma estratégia para o perfil cliente.
- **Fazer funcionar antes de polir.** Se ArchUnit reprovar, conserte; nunca desabilite teste para passar o build.
- **Nunca `git add .` ou `git add -A`:** adicione por pasta ou arquivo.

## Convenção de commit por skill

Um commit por skill, só com o DoD verde:

```
feat(trader-XX): <resumo do que a skill produziu>

<3 a 6 linhas sobre o que agora existe>

DoD atingido:
- <verificação 1>
- <verificação 2>

Ref: skills/trader-XX-<nome>/SKILL.md
```

Prefixos: `feat(trader-XX)`, `fix(trader-XX)` (correção retroativa), `docs(trader-XX)` (spec/revisão da skill antes do DoD), `docs:`, `chore:`.

## Push e PR automáticos ao fim da skill (D-18)

Fluxo padrão de uma skill:

1. Branch `trader-XX-nome` a partir da `main`.
2. Rascunho e revisão do `SKILL.md` com o Roberto (pode subir como `docs(trader-XX)` sem PR).
3. Implementação na mesma branch, com commits intermediários livres.
4. **Ao fechar DoD verde:** atualizar `README.md` ("Estado atual") e `CLAUDE.md` ("Onde paramos") no mesmo commit `feat(trader-XX)`; em seguida, `git push -u origin trader-XX-nome` e `gh pr create` para `main`. Nada disso espera pedido manual.
5. Merge é do Roberto. Nunca force push, nunca `gh pr merge` sozinho.

## Ao fechar uma sessão

Antes do commit da sessão, atualizar: a tabela "Estado atual" do `README.md` e a seção "Onde paramos" do `CLAUDE.md`.
