---
name: trader-06-frontend-grafico
description: Gráfico de candles no frontend. nucleo-backend ganha um proxy GET /api/mercado/candles que repassa para o motor-quant (com shared secret), autorizado por VER_OPERACAO_TEMPO_REAL. frontend-app ganha a feature `mercado/` com a lib `lightweight-charts`; a OperacaoPage da skill 05 é redesenhada com o gráfico como protagonista e um painel lateral que agrupa dados da operação, contador e chat ao vivo. Linhas horizontais marcam entrada, stop e cada alvo. Sexta skill a focar no frontend; roda depois da trader-05-operacoes-tempo-real.
---

# trader-06-frontend-grafico — Gráfico ao vivo com operação desenhada

## Contexto

Dá à plataforma a cara do produto: no detalhe da operação, o cliente vê o gráfico do ativo em tempo real com entrada, stop e alvos marcados como linhas horizontais; ao lado, o resumo da operação, o contador de participantes ao vivo e o chat da skill 05.

Para o browser não quebrar a regra D-23 (motor-quant só aceita header interno), o `nucleo-backend` ganha um proxy mínimo em `/api/mercado/candles` que repassa a chamada ao `motor-quant` adicionando `X-INTERNAL-TOKEN` — a autenticação do browser continua sendo cookie + direito `VER_OPERACAO_TEMPO_REAL`. Indicadores e canais STOMP de dados de mercado ficam para skills futuras: aqui entregamos candles e as linhas da operação.

**Assume:** skill 05 concluída. `OperacaoPage` da feature `operacoes/` já existe, `useOperacao` responde, `motor-quant` sobe pelo compose com seed sintético (NAS100 e XAUUSD, M1) e `MOTOR_SHARED_TOKEN` configurado.

## Referências obrigatórias

- `../trader-fullstack/SKILL.md`
- `../trader-fullstack/references/decisoes-tomadas.md` — D-06 (stack), D-07 (Lightweight Charts), D-14 (versões), D-19 (fetch wrapper), D-20 (Tailwind), D-21 (feature-based), D-22 (fonte de dados), D-23 (shared secret), D-28 (proxy), D-29 (linhas horizontais), D-30 (OperacaoPage com gráfico)
- `../trader-fullstack/references/arquitetura.md` — `motor-quant` e `nucleo-backend` (módulo `shared` ganha o cliente HTTP do motor)
- `../trader-fullstack/references/visao-e-requisitos.md` — RF-03 (gráfico de ativos), RF-05 (operação ao vivo no gráfico), RF-06 (contador)

## Passos

### Backend — proxy `/api/mercado/candles` (D-28)

1. **Config nova** em `TraderProperties.Motor`:
   - `baseUrl` (ex: `http://motor-quant:8091` em docker, `http://localhost:8091` em dev).
   - `sharedToken` (lido de `MOTOR_SHARED_TOKEN`).
   - `timeoutSegundos` (default 5).
   - `application-dev.yml`, `application-docker.yml` e `application-test.yml` ganham essas chaves.

2. **Módulo `com.traderoperation.mercado`** (hexagonal lite — só o essencial):
   - `domain/Candle.java` (record `ts`, `abertura`, `maxima`, `minima`, `fechamento`, `volume`), `Timeframe` (enum `M1, M5, M15, H1, D1`), `Ativo` (enum fechado `NAS100`, `XAUUSD`).
   - `application/port/in/ConsultarCandlesQuery` (`listar(ativo, timeframe, limite) -> List<Candle>`).
   - `application/port/out/MotorQuantPort` (`candles(ativo, timeframe, limite) -> List<Candle>`).
   - `application/service/MercadoService` implementa a query; não grava nada, só repassa.
   - `infrastructure/adapter/in/web/MercadoController`:
     - `GET /api/mercado/candles?ativo=NAS100&timeframe=M1&limite=200` → `@PreAuthorize("hasAuthority('VER_OPERACAO_TEMPO_REAL')")`; `limite` entre 1 e 2000, default 200.
   - `infrastructure/adapter/out/motor/MotorQuantHttpAdapter`: usa `RestClient` do Spring 6; adiciona `X-INTERNAL-TOKEN`; traduz erro 401/403 do motor para `IllegalStateException` (sinaliza config errada), 5xx para erro genérico.
   - **Nada de persistência**. Caching entra em uma skill futura quando o volume justificar.

3. **Testes**:
   - `MercadoControllerTest` (`@WebMvcTest`) com `MotorQuantPort` mockado: cliente FREE (sem `VER_OPERACAO_TEMPO_REAL`) → 403; cliente PRO → 200.
   - `MotorQuantHttpAdapterTest` com `MockRestServiceServer`: envia `X-INTERNAL-TOKEN`, trata 403 do motor.
   - ArchUnit: adiciona `"mercado"` em `MODULOS`.

### Frontend — feature `mercado/` + redesign da OperacaoPage (D-30)

4. **Nova dependência** no `frontend-app`: `lightweight-charts@5.0.3` (versão estável recente, licença Apache 2.0, D-07).

5. **Feature `mercado/`**:
   - `api/mercadoApi.ts`:
     ```
     export type Timeframe = 'M1' | 'M5' | 'M15' | 'H1' | 'D1';
     export interface Candle { ts: string; abertura: number; maxima: number; minima: number; fechamento: number; volume: number; }
     mercadoApi = { listarCandles(ativo, timeframe, limite?) }
     ```
   - `hooks/useCandles.ts`: TanStack Query key `['mercado','candles',ativo,timeframe,limite]`, `staleTime: 15_000`.
   - `componentes/GraficoCandles.tsx`:
     - Monta `createChart` do `lightweight-charts` no `useEffect`, cria `addCandlestickSeries`, atualiza com `setData` quando `candles` muda.
     - Props: `candles`, `linhas?: ReadonlyArray<{ preco: number; cor: string; rotulo: string }>` → cada linha vira `priceLine` via `createPriceLine` no series; linhas são removidas (`removePriceLine`) e recriadas quando mudam.
     - Theme escuro casando com `styles.css` (fundo `#0f172a`, texto `#e2e8f0`, grid `#334155`).
     - Resize listener via `ResizeObserver` no container.
     - Cleanup no `return` do effect (`chart.remove()`).
   - `componentes/EscolhaTimeframe.tsx`: dropdown com `M1`, `M5`, `M15`, `H1`, `D1` (fallback padrão M1); mantém o valor no `useState` da OperacaoPage.

6. **OperacaoPage reformada** (`features/operacoes/pages/OperacaoPage.tsx`):
   - Layout `grid gap-6 lg:grid-cols-[1fr_320px]`:
     - **Coluna principal**: cabeçalho (ativo + direção + status + contador), `GraficoCandles` com linhas de entrada/stop/alvos (D-29), `EscolhaTimeframe` acima do gráfico.
     - **Painel lateral** (`lg:col-span-1`): bloco de dados da operação (entrada/stop/alvos/estratégia/resultado), botão de "Encerrar" quando aplicável, chat da operação (existente).
   - Linhas (D-29):
     - Entrada: `cor acento` (azul `#38bdf8`), rótulo `E @ <valor>`.
     - Stop: `cor erro` (vermelho `#ef4444`), rótulo `S @ <valor>`.
     - Alvos: cada um na `cor sucesso` (verde `#22c55e`), rótulo `A1 @ <valor>`, `A2 @ <valor>`, etc.
   - Em telas estreitas (<lg), layout vira 1 coluna com gráfico no topo, dados logo abaixo e chat por último.

7. **Testes frontend**:
   - `mercadoApi.test.ts`: chama `GET /api/mercado/candles` com os parâmetros corretos.
   - `useCandles.test.tsx`: devolve dados quando a API responde; não dispara quando `ativo` ou `timeframe` ausente.
   - `GraficoCandles.test.tsx` básico: renderiza o `<div>` do container; não quebra quando `candles` está vazio. (O `lightweight-charts` manipula um `<canvas>` que jsdom não renderiza de verdade; só confirmamos que não lança.)

### Fluxo end-to-end do DoD

8. **Compose up**: nucleo-backend + motor-quant + frontend. Via UI:
   - Login do master → publica NAS100 COMPRA 20000 / stop 19900 / alvos 20100, 20200.
   - Login do cliente PRO (outra aba) → abre `/operacoes/:id`.
   - Gráfico aparece com candles do seed sintético (D-22) do motor-quant.
   - Linhas horizontais marcam entrada/stop/alvos.
   - Chat recebe mensagens em tempo real.
   - Master encerra → operação some de "ao vivo" e a página do cliente mostra "ENCERRADA" + resultado.

## Definition of Done (verificável)

```bash
# Backend
cd nucleo-backend && ./mvnw test && cd ..   # todos verdes, ArchUnit com "mercado"

# Frontend
cd frontend-app && npm run typecheck && npm run lint && npm run test:ci && npm run build && cd ..

# Compose
docker compose up -d --build
./scripts/verificar-infra.sh

# Smoke do proxy (como cliente PRO)
curl -s -c c.txt http://localhost:8090/api/auth/csrf > /dev/null
TK=$(grep XSRF-TOKEN c.txt | awk '{print $7}')
curl -s -b c.txt -c c.txt -H "X-XSRF-TOKEN:$TK" -H "Content-Type: application/json" \
  -d '{"email":"cliente@trader.local","senha":"trader123"}' http://localhost:8090/api/auth/login > /dev/null
curl -sf -b c.txt "http://localhost:8090/api/mercado/candles?ativo=NAS100&timeframe=M1&limite=5" \
  | python3 -c "import json,sys;d=json.load(sys.stdin);print('candles:',len(d),'ultimo fechamento:',d[-1]['fechamento'])"
```

Esperado: 5 candles com fechamentos numéricos.

Teste manual na UI (documentado em `frontend-app/README.md`):
- `/operacoes/:id` mostra gráfico com candles + linhas entrada/stop/alvos + chat lateral.
- Trocar `Timeframe` recarrega candles sem perder o chat.
- Resize da janela redimensiona o gráfico sem quebrar.

## Pré-requisitos

Nenhum novo. `MOTOR_SHARED_TOKEN` já existe no `.env` desde a skill 04.

## Notas para as próximas skills

- **Skill 07 (IA analista)** adiciona `/api/mercado/indicadores` (mesmo padrão de proxy) e um painel de IA ao lado do chat.
- **Skill 10 (conector MT5)** passa a alimentar candles reais via `POST /api/motor/ingest/candles`; o proxy desta skill continua igual.
- **Skill 11 (Copiloto Electron)** reusa `GraficoCandles` e `useCandles` dentro do app do cliente.
- Caching de candles (Redis TTL curto) e canal STOMP `/topic/mercado/*` entram quando o volume de clientes justificar — pensamos em escala só depois que tiver cliente.
