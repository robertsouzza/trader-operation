# Trader Operation — contexto para o Claude

Plataforma web de análise e operação em tempo real com IA, integrada ao MetaTrader 5. Usuário: Roberto. Idioma das conversas, docs e commits: português (BR). Código em inglês.

## Antes de implementar

1. Leia `skills/trader-fullstack/SKILL.md` (índice, roadmap, regras, convenção de commit).
2. Leia `skills/trader-fullstack/references/decisoes-tomadas.md`. Ele vence qualquer conflito.
3. Execute só a skill numerada da vez; não pule o Definition of Done.
4. Ambiguidade não coberta pelas referências: pare e pergunte. A resposta vira uma decisão `D-XX` nova.

## Regras inegociáveis

- Nenhuma ordem chega ao MT5 sem passar pelo motor de risco; a IA não tem ferramenta para alterá-lo.
- Regras de estratégia nunca saem da nuvem para o perfil cliente.
- Nada de dados reais, chaves de API, senhas de MT5 ou `.env` no repositório.
- Nunca `git add .` / `git add -A`; nunca desabilitar teste (incluindo ArchUnit) para passar o build.
- Nunca force push, nunca mesclar PR — o merge é sempre do Roberto.
- **Fim de skill (DoD verde) = commit + push + PR automáticos** (D-18): no mesmo passo que atualiza "Estado atual" e "Onde paramos", a branch `trader-XX-nome` sobe para o `origin` e um PR para `main` é aberto, sem precisar pedir. Spec intermediária (só o `SKILL.md`, por exemplo) pode subir sem PR; o PR só nasce com o DoD verde.

## Ambiente

- Repositório em `~/trader-operation` no WSL2.
- `conector/` roda com o Python do **Windows** (o pacote MetaTrader5 não existe para Linux); comunicação com o WSL por `localhost`.

## Onde paramos

- **03/out/2026:** skill 00 concluída (monorepo, referências, infra Postgres+pgvector, Redis, S3/SeaweedFS).
- **03/out/2026:** skill 01 concluída (`nucleo-backend`: shared, auditoria, autenticação por cookie JWT + CSRF, ArchUnit, 23 testes).
- **09/out/2026:** `SKILL.md` da `trader-02-planos-perfis` escrito e aprovado pelo Roberto; decisões D-15 (Direito enum), D-16 (Assinatura já aqui), D-17 (admin API mínima) e D-18 (push + PR automáticos no fim de skill) registradas.
- **09/out/2026:** skill 02 concluída (`planos`: enum `Direito` + `MatrizDeDireitos`, entidade `Assinatura`, endpoints `/api/planos`, `/api/me/plano`, `/api/admin/usuarios`, filtro JWT populando authorities por direito, ArchUnit com módulo novo + regra blindando o enum, 41 testes). Próxima: `trader-03-frontend-core` (React, login por cookie, rotas por perfil + direito).

## Ao fechar uma sessão

Atualize a tabela "Estado atual" do `README.md` e a seção "Onde paramos" acima, no mesmo commit da sessão. Se a sessão fechou uma skill (DoD verde), siga D-18: commit + push + PR.
