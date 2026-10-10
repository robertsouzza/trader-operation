---
name: trader-03-frontend-core
description: Cria o frontend-app do Trader Operation — React 19 + TypeScript + Vite + React Router + TanStack Query + Vitest + Tailwind CSS, organizado em feature-based (shared, autenticacao, planos, admin). Cliente HTTP por fetch com CSRF e cookies httpOnly, Context de usuário atual, rotas protegidas por perfil + direito, telas de login, home, catálogo público de planos, "Meu plano" e lista administrativa de usuários com troca de plano. Quarta skill do roadmap; roda depois da trader-02-planos-perfis.
---

# trader-03-frontend-core — React: login por cookie, layout, rotas por perfil e plano

## Contexto

Cria o `frontend-app/` com o esqueleto do React do produto: boot (Vite + Router + QueryClient), cliente HTTP com CSRF, Context de usuário atual, rotas protegidas por perfil + direito e as telas mínimas que a skill 02 já tem backend para servir — login, catálogo de planos, "Meu plano" e administração de usuários (lista + trocar plano). Nenhum gráfico de ativo, nenhuma operação em tempo real, nenhum chat, nenhuma IA — isso vem nas skills 05, 06 e 07.

**Assume:** skill 02 concluída. Backend em `http://localhost:8090` com `/api/auth/*`, `/api/planos`, `/api/me/plano`, `/api/admin/usuarios` operando e CORS liberado para `http://localhost:5173`.

## Referências obrigatórias

- `../trader-fullstack/SKILL.md`
- `../trader-fullstack/references/decisoes-tomadas.md` — D-06 (stack), D-10 (auth), D-14 (versões), D-15 (nomes de direito), D-19 (HTTP via fetch wrapper), D-20 (Tailwind CSS), D-21 (feature-based)
- `../trader-fullstack/references/arquitetura.md` — `frontend-app`
- `../trader-fullstack/references/visao-e-requisitos.md` — RF-01 (perfil e plano visíveis), RNF-02 (nenhum segredo armazenado no cliente)

## Passos

1. **Projeto Vite em `frontend-app/`** (`npm create vite@latest` manual, não via skill), com versões pinadas (D-14):
   - Runtime: `react@19`, `react-dom@19`, `react-router-dom@6`, `@tanstack/react-query@5`, `@tanstack/react-query-devtools@5`.
   - Dev: `vite@6`, `@vitejs/plugin-react`, `vitest@3`, `@testing-library/react@16`, `@testing-library/jest-dom@6`, `@testing-library/user-event@14`, `jsdom`, `@types/react`, `@types/react-dom`, `@types/node`, `typescript@5`, `eslint@9`, `@typescript-eslint/*`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, `tailwindcss@4.1`, `@tailwindcss/vite@4.1`, `autoprefixer`, `postcss`.
   - Vitest 3 pede Vite 6; o par Tailwind 4.1 é o primeiro que aceita Vite 6 sem bug de build; `@tailwindcss/vite@4.0.0` falha em `build` com "Cannot convert undefined or null to object".
   - Dois configs separados: `vite.config.ts` (sem campo `test`) e `vitest.config.ts` (via `defineConfig` de `vitest/config`), para evitar que o `UserConfig` do Vite 6 brigue com o `UserConfig` do Vite interno do Vitest.
   - Scripts no `package.json`: `dev`, `build`, `preview`, `test`, `test:ci` (`vitest run --coverage`), `typecheck` (`tsc --noEmit`), `lint`.
   - Dev server na porta **5173** (padrão Vite, alinhado com `TRADER_CORS_ORIGENS` do backend).
   - `.env.example` com `VITE_API_BASE_URL=http://localhost:8090`. Nunca commitar `.env`.

2. **TypeScript estrito** em `tsconfig.json`: `strict: true`, `noUncheckedIndexedAccess: true`, `exactOptionalPropertyTypes: true`, `moduleResolution: "bundler"`, `jsx: "react-jsx"`, path alias `@/* -> src/*`.

3. **Tailwind CSS (D-20)**:
   - `vite.config.ts` com `@tailwindcss/vite`.
   - `src/styles.css` com `@tailwind base; @tailwind components; @tailwind utilities;` e um `@layer base` definindo cor de fundo, cor de texto base e `font-family`.
   - `tailwind.config.ts` com `content: ['./index.html', './src/**/*.{ts,tsx}']`, `theme.extend.colors` com paleta sóbria (grafite/azul do SGCE) e `fontFamily.sans: ['Inter', 'system-ui', 'sans-serif']`.
   - Sem lib de componentes nesta skill. Botão, campo e diálogo são escritos à mão em `shared/componentes/` com classes Tailwind.

4. **Estrutura de pastas (D-21)** sob `src/`:
   ```
   src/
   ├── main.tsx                      (BrowserRouter + QueryClientProvider + UsuarioAtualProvider)
   ├── App.tsx                       (árvore de rotas)
   ├── styles.css
   ├── shared/
   │   ├── api/
   │   │   ├── apiClient.ts          (fetch wrapper com CSRF + credentials)
   │   │   ├── csrf.ts               (lê/garante cookie XSRF-TOKEN)
   │   │   └── erros.ts              (NaoAutorizadoError, AcessoNegadoError, ErroApi)
   │   ├── layout/
   │   │   ├── Layout.tsx
   │   │   ├── Cabecalho.tsx         (nome + perfil + plano + sair)
   │   │   └── MenuLateral.tsx       (itens visíveis por direito)
   │   ├── componentes/
   │   │   ├── Botao.tsx
   │   │   ├── CampoTexto.tsx
   │   │   ├── Dialogo.tsx
   │   │   └── Carregando.tsx
   │   └── seguranca/
   │       ├── UsuarioAtualProvider.tsx   (Context + hook useUsuarioAtual())
   │       ├── RotaProtegida.tsx          (precisa estar autenticado)
   │       ├── RotaPorPerfil.tsx          (perfil obrigatório)
   │       └── RotaPorDireito.tsx         (direito obrigatório)
   ├── features/
   │   ├── autenticacao/
   │   │   ├── api/autenticacaoApi.ts     (login, logout, me)
   │   │   ├── hooks/useLogin.ts, useLogout.ts
   │   │   └── pages/LoginPage.tsx
   │   ├── planos/
   │   │   ├── api/planosApi.ts           (catalogo, meuPlano)
   │   │   ├── hooks/useCatalogo.ts, useMeuPlano.ts
   │   │   └── pages/CatalogoPlanosPage.tsx, MeuPlanoPage.tsx
   │   ├── admin/
   │   │   ├── api/adminUsuariosApi.ts    (listar, trocarPlano)
   │   │   ├── hooks/useListarUsuarios.ts, useTrocarPlano.ts
   │   │   └── pages/ListaUsuariosPage.tsx + componentes/DialogoTrocarPlano.tsx
   │   └── home/
   │       └── pages/HomePage.tsx
   └── test/
       └── setup.ts                       (@testing-library/jest-dom)
   ```

5. **Cliente HTTP (D-19)** — `shared/api/apiClient.ts`:
   - `apiClient.get<T>(path)`, `.post<T>(path, body)`, `.put<T>(path, body)`, `.del(path)`.
   - Sempre inclui `credentials: 'include'` e `Content-Type: application/json` em requisições com corpo.
   - Em mutações (POST/PUT/PATCH/DELETE), chama `garantirCsrf()` (que faz `GET /api/auth/csrf` se o cookie `XSRF-TOKEN` ainda não existir), lê o cookie e injeta o header `X-XSRF-TOKEN`.
   - 401 → lança `NaoAutorizadoError`. 403 → `AcessoNegadoError`. Outros → `ErroApi` com `mensagem` e `status`.
   - **Nunca** acessa `localStorage` ou `sessionStorage`; o token vive em cookie httpOnly e não é lido pelo JS (RNF, D-10).

6. **TanStack Query**:
   - `QueryClient` em `main.tsx` com `defaultOptions`: `queries.retry: 1`, `queries.staleTime: 60_000`, `queries.refetchOnWindowFocus: false`.
   - Query keys padronizadas: `['usuarioAtual']`, `['meuPlano']`, `['catalogoPlanos']`, `['adminUsuarios', { q, pagina, tamanho }]`.
   - Mutações invalidam as queries necessárias (ex.: `useLogin` invalida `['usuarioAtual']` e `['meuPlano']`; `useTrocarPlano` invalida `['adminUsuarios']`).
   - DevTools do React Query só em `import.meta.env.DEV`.

7. **Usuário atual** (`shared/seguranca/UsuarioAtualProvider.tsx`):
   - Dispara `GET /api/auth/me` e `GET /api/me/plano` em paralelo via `useQueries`.
   - Expõe `useUsuarioAtual()` → `{ usuario, plano, direitos: Set<string>, carregando, naoAutenticado }`.
   - Quando qualquer das queries falha com `NaoAutorizadoError`, marca `naoAutenticado = true` e deixa a `RotaProtegida` redirecionar.

8. **Rotas protegidas** (`shared/seguranca/`):
   - `<RotaProtegida>`: se `naoAutenticado` → `<Navigate to="/login" replace state={{ de: location.pathname }}/>`.
   - `<RotaPorPerfil perfil="ADMIN">`: se perfil ≠ → `<Navigate to="/sem-permissao" />`.
   - `<RotaPorDireito direito="ADMINISTRAR_USUARIOS">`: se o `Set<string> direitos` não contém → `/sem-permissao`.
   - Todas renderizam `<Carregando />` enquanto `carregando`.

9. **Árvore de rotas** em `App.tsx`:
   ```
   /login                        → LoginPage (sem layout)
   /sem-permissao               → tela 403 amigável
   /   (dentro de <Layout>)
     ├── /                       → HomePage (RotaProtegida)
     ├── /planos                 → CatalogoPlanosPage (público; mostra layout quando logado)
     ├── /meu-plano              → MeuPlanoPage (RotaProtegida)
     └── /admin/usuarios         → ListaUsuariosPage (RotaPorDireito="ADMINISTRAR_USUARIOS")
   *                            → NaoEncontradaPage (404)
   ```
   A `CatalogoPlanosPage` é renderizável sem login (D-10 permite GET anônimo em `/api/planos`); se o usuário estiver logado, aparece com o layout do app, senão com um layout minimal.

10. **Telas desta skill**:
    - **LoginPage**: form com e-mail e senha. Em falha, mensagem genérica "E-mail ou senha inválidos." (preserva a resposta do backend). Em sucesso, redireciona para `location.state.de ?? '/'`.
    - **HomePage**: saudação ("Olá, <nome>"), cartões de atalho renderizados conforme direitos (ex.: cartão "Operações ao vivo" se `VER_OPERACAO_TEMPO_REAL`, "Chat com a IA" se `USAR_CHAT_IA`, "Backtest de estratégia" se `PEDIR_BACKTEST_IA`, "Admin de usuários" se `ADMINISTRAR_USUARIOS`). Nada clicável leva a tela existente nesta skill além de `/admin/usuarios` e `/meu-plano` — os outros avisam "em breve".
    - **CatalogoPlanosPage**: três colunas Free/Pro/Premium com a lista de direitos de cada uma; destaque no plano atual se o usuário estiver logado.
    - **MeuPlanoPage**: plano atual, data de início e fim (ou "não expira"), origem, e a lista efetiva de direitos liberados.
    - **ListaUsuariosPage**: campo de busca (debounced 300ms), tabela paginada (`pagina`, `tamanho=20`), botão "Trocar plano" em cada linha. Diálogo com `select` do plano (FREE/PRO/PREMIUM) e campo opcional de "Duração (dias)". Em sucesso: toast de confirmação e refetch da lista.

11. **Layout e navegação**:
    - **Cabecalho**: logo "Trader Operation", nome + perfil (badge) + plano (badge), botão "Sair" (chama `useLogout` → `POST /api/auth/logout` → invalida `['usuarioAtual']` → redireciona para `/login`).
    - **MenuLateral**: links visíveis conforme direitos. Mobile-first: colapsa em menu hamburger abaixo de 768px.
    - **Layout**: grid com cabeçalho fixo, menu lateral à esquerda em telas largas, conteúdo central com `max-w-6xl mx-auto p-6`.
    - **Formatação**: datas via `Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' })`; textos sempre em pt-BR.

12. **Testes (Vitest + Testing Library)**:
    - `shared/api/apiClient.test.ts` (unit): `fetch` mockado via `vi.spyOn(window, 'fetch')`. Verifica `credentials:'include'`, `X-XSRF-TOKEN` só em POST/PUT/DELETE, chamada a `/api/auth/csrf` quando não há cookie, e os três tipos de erro (401/403/outro).
    - `features/autenticacao/pages/LoginPage.test.tsx` (component): envia e-mail e senha; em 401 mostra "E-mail ou senha inválidos.".
    - `shared/seguranca/RotaPorDireito.test.tsx`: redireciona para `/sem-permissao` quando falta direito; renderiza filhos quando presente.
    - `features/planos/hooks/useCatalogo.test.ts`: hook retorna os três planos quando a API responde.
    - `features/admin/pages/ListaUsuariosPage.test.tsx`: mostra 3 linhas, abre diálogo e dispara `useTrocarPlano`.
    - Cobertura mínima: cada feature tem pelo menos um teste; o `shared/api` 100% das funções exportadas.

13. **ESLint + typecheck**: `npm run lint` e `npm run typecheck` passam. Config do ESLint ativa `react-hooks/rules-of-hooks`, `react-hooks/exhaustive-deps`, `@typescript-eslint/no-unused-vars` (warn), `@typescript-eslint/consistent-type-imports`.

14. **Dockerfile.dev para o compose** (`frontend-app/Dockerfile.dev`):
    - Multi-stage: `node:20-alpine` para `npm ci && npm run build`; `nginx:1.27-alpine` servindo `dist/` com `try_files $uri /index.html` para o SPA.
    - Serviço `frontend-app` no `docker-compose.yml`: porta `${TRADER_FRONTEND_PORT:-5173}:80`, `depends_on: nucleo-backend`. Variável `TRADER_FRONTEND_PORT` adicionada ao `.env.example`.
    - Em dev local do dia a dia o fluxo recomendado é `npm run dev` direto no WSL; o Dockerfile existe para fechar o compose do produto.

## Definition of Done (verificável)

```bash
# 1. Lint, typecheck e testes do frontend
cd frontend-app
npm ci
npm run typecheck
npm run lint
npm run test:ci
npm run build
cd ..

# 2. Backend no ar (skill 02 continua passando)
docker compose up -d --build nucleo-backend postgres redis s3 s3-init
./scripts/verificar-infra.sh

# 3. Dev server do frontend
cd frontend-app && npm run dev &
# abrir http://localhost:5173 e seguir o roteiro manual abaixo
```

Roteiro manual na UI (documentado no `frontend-app/README.md`):

1. Acessar `/` sem login → redireciona para `/login`.
2. Logar como `master@trader.local` / `trader123`.
3. Em `/meu-plano`: aparece FREE (padrão, sem assinatura) e lista de direitos vindos do perfil MASTER (inclui `PUBLICAR_OPERACAO`, `USAR_CHAT_IA`).
4. Em `/planos`: três colunas com os direitos de cada plano.
5. Em `/admin/usuarios`: redireciona para `/sem-permissao` (master não tem `ADMINISTRAR_USUARIOS`).
6. Fazer logout e logar como `admin@trader.local`.
7. `/admin/usuarios`: lista 3 usuários do seed. Trocar o plano do cliente para `PREMIUM` pelo diálogo.
8. Logout e login como `cliente@trader.local`.
9. `/meu-plano` mostra `PREMIUM` com `PEDIR_BACKTEST_IA` e `PUBLICAR_NA_VITRINE` entre os direitos.
10. Botão "Sair" apaga o cookie e volta para `/login`.

ArchUnit e verify do backend continuam passando. Nenhum segredo entra em `.env` do frontend; só `VITE_API_BASE_URL`.

## Pré-requisitos na máquina de dev

Node 20+ no WSL (`node -v`). Se não houver, instalar via `nvm install 20`. Nenhum outro pré-requisito.

## Notas para as próximas skills

- **Skill 05 (operações)** reutiliza `apiClient` e adiciona uma camada WebSocket/STOMP em `shared/tempo-real/` consumindo o backend. Também cria `features/operacoes/`.
- **Skill 06 (frontend gráfico)** instala `lightweight-charts` dentro de `features/operacoes/`.
- **Skill 07 (IA analista)** vira a feature `features/ia/` com `/api/ia/chat`.
- **Skill 11 (Copiloto Electron)** reaproveita `shared/api`, `shared/componentes` e `shared/seguranca` do `frontend-app` como workspace.
- A tela de pagamento/assinatura entra na skill 09 como feature `features/pagamentos/`; o endpoint `PUT /api/admin/usuarios/{id}/plano` não é reusado por lá.
