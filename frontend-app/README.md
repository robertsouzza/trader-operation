# frontend-app

Frontend web do Trader Operation (React 19 + TypeScript + Vite + React Router + TanStack Query + Vitest + Tailwind CSS). Entregue pela skill 03.

## Pré-requisitos

- Node 20+ (no WSL para o dev local).
- Backend em `http://localhost:8090` com o seed de dev (ver `nucleo-backend/`).

## Rodar

```bash
cp .env.example .env           # ajuste VITE_API_BASE_URL se precisar
npm ci
npm run dev                    # http://localhost:5173
```

Outros scripts:

```bash
npm run typecheck
npm run lint
npm run test:ci
npm run build && npm run preview
```

## Roteiro manual (DoD da skill 03)

Com o backend no ar (`docker compose up -d --build nucleo-backend postgres redis s3 s3-init`) e o `npm run dev` rodando:

1. Acessar `http://localhost:5173/` sem login → redireciona para `/login`.
2. Login `master@trader.local` / `trader123` → cai em `/`.
3. `/meu-plano` → mostra `FREE` (sem assinatura) e direitos do perfil MASTER (`PUBLICAR_OPERACAO`, `USAR_CHAT_IA` etc.).
4. `/planos` → três colunas FREE/PRO/PREMIUM com os direitos de cada.
5. `/admin/usuarios` → `/sem-permissao` (master não tem `ADMINISTRAR_USUARIOS`).
6. Logout, login `admin@trader.local` / `trader123`.
7. `/admin/usuarios` → lista 3 usuários do seed. Botão "Trocar plano" do cliente → diálogo com select e duração; muda para `PREMIUM`.
8. Logout, login `cliente@trader.local` / `trader123`.
9. `/meu-plano` → `PREMIUM` com `PEDIR_BACKTEST_IA` e `PUBLICAR_NA_VITRINE` entre os direitos.
10. Botão "Sair" apaga cookie e volta para `/login`.

## Produção (via docker compose)

```bash
docker compose up -d --build frontend-app
# nginx servindo em http://localhost:5173
```

Nenhum segredo entra no `.env` do frontend; só `VITE_API_BASE_URL`.
