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
- Commit e push só quando o Roberto pedir ou quando a skill estiver com DoD verde, seguindo a convenção do índice.

## Ambiente

- Repositório em `~/trader-operation` no WSL2.
- `conector/` roda com o Python do **Windows** (o pacote MetaTrader5 não existe para Linux); comunicação com o WSL por `localhost`.

## Onde paramos

- **03/out/2026:** skill 00 concluída (monorepo, referências, infra Postgres+pgvector, Redis, S3/SeaweedFS). Próxima: escrever e revisar com o Roberto o `SKILL.md` da `trader-01-nucleo-core`.

## Ao fechar uma sessão

Atualize a tabela "Estado atual" do `README.md` e a seção "Onde paramos" acima, no mesmo commit da sessão.
