---
name: trader-05-operacoes-tempo-real
description: Operações publicadas e chat ao vivo no Trader Operation. Backend ganha módulos `operacoes` e `chat` com migrações Flyway, REST para publicar/listar/encerrar/comentar, Spring WebSocket + STOMP autenticado pelo cookie JWT, Redis pub/sub para broadcasting e contador de participantes por operação. Frontend ganha a feature `operacoes` com lista, detalhe, chat em tempo real e botão de publicar/encerrar para o master. Sexta skill do roadmap; roda depois da trader-04-motor-quant-base.
---

# trader-05-operacoes-tempo-real — Operações publicadas, STOMP e chat

## Contexto

Adiciona a camada ao vivo do produto: o master publica uma operação (ativo, direção, entrada, stop, alvos, estratégia); clientes com `VER_OPERACAO_TEMPO_REAL` recebem instantaneamente via STOMP, conversam entre si no chat da operação e aparecem no contador de participantes. FREE (direito `VER_OPERACAO_COM_ATRASO`) só vê as operações via REST com atraso de 15 min. O motor-quant **não** é chamado nesta skill — a aferição de preço contra stop/alvo fica para a skill 10 quando o conector real chegar.

**Assume:** skill 04 concluída. Backend, frontend e motor-quant estão rodando pelo `docker compose up`; os direitos da MatrizDeDireitos (`PUBLICAR_OPERACAO`, `VER_OPERACAO_TEMPO_REAL`, `LER_CHAT_OPERACAO`, `PARTICIPAR_CHAT_OPERACAO`, `VER_OPERACAO_COM_ATRASO`) já estão disponíveis no SecurityContext como authorities.

## Referências obrigatórias

- `../trader-fullstack/SKILL.md`
- `../trader-fullstack/references/decisoes-tomadas.md` — D-02 (sinal publicado, execução pelo cliente), D-09 (Redis pub/sub + STOMP), D-15 (direitos como enum), D-25 (STOMP), D-26 (persistência Postgres + Redis para pub/sub e contador), D-27 (encerramento manual pelo master)
- `../trader-fullstack/references/arquitetura.md` — módulos `operacoes`, `chat`, `tempo_real`
- `../trader-fullstack/references/visao-e-requisitos.md` — RF-04, RF-05, RF-06, RF-07, RF-14

## Passos

### Backend

1. **Dependência nova** no `nucleo-backend/pom.xml`: `spring-boot-starter-websocket`.

2. **Módulo `com.traderoperation.operacoes`** (hexagonal):
   - `domain/`: `Operacao` (record imutável), `Direcao` (`COMPRA`/`VENDA`), `StatusOperacao` (`PUBLICADA`/`ENCERRADA`), `ResultadoOperacao` (`GAIN`/`LOSS`/`NEUTRO`/`INDEFINIDO`).
   - `application/port/in`: `PublicarOperacaoUseCase`, `EncerrarOperacaoUseCase`, `ConsultarOperacoesQuery`, `ConsultarOperacaoQuery`, DTOs `OperacaoResumo`, `OperacaoDetalhe`, `PublicarOperacaoCmd`, `EncerrarOperacaoCmd`.
   - `application/port/out`: `OperacaoRepositoryPort`, `OperacaoEventosPort` (publica eventos em Redis).
   - `application/service`: `OperacaoService` (`@Auditavel("PUBLICAR_OPERACAO")` e `@Auditavel("ENCERRAR_OPERACAO")`).
   - `infrastructure/adapter/in/web`: `OperacoesController` com:
     - `POST /api/operacoes` → `@PreAuthorize("hasAuthority('PUBLICAR_OPERACAO')")`
     - `POST /api/operacoes/{id}/encerrar` → mesma authority; `OperacaoService` valida que o master logado é o dono.
     - `GET /api/operacoes/ao-vivo` → filtra conforme direito do solicitante:
       - `VER_OPERACAO_TEMPO_REAL` → todas as `PUBLICADA`.
       - `VER_OPERACAO_COM_ATRASO` → só `PUBLICADA` com `publicada_em <= now() - interval '15 min'`.
       - Nenhum dos dois → 403.
     - `GET /api/operacoes/{id}` → mesma regra de visibilidade.
   - `infrastructure/adapter/out/persistence`: `OperacaoJpaEntity` + repositório + adapter.
   - `infrastructure/adapter/out/eventos`: `OperacaoEventosRedisAdapter` publicando em canais `trader:operacoes:publicada` e `trader:operacoes:encerrada`.

3. **Módulo `com.traderoperation.chat`** (hexagonal):
   - `domain/`: `Mensagem` (record), limite 500 caracteres.
   - `application/port/in`: `EnviarMensagemUseCase`, `ListarMensagensQuery`.
   - `application/port/out`: `MensagemRepositoryPort`, `MensagemEventosPort`.
   - `application/service`: `ChatService` com `@Auditavel("ENVIAR_MENSAGEM")`. Antes de enviar, verifica que a operação existe e está `PUBLICADA` (via port in de `operacoes`).
   - `infrastructure/adapter/in/web`: `ChatController`:
     - `GET /api/operacoes/{id}/mensagens?limite=100` → `@PreAuthorize("hasAuthority('LER_CHAT_OPERACAO')")`.
     - `POST /api/operacoes/{id}/mensagens` body `{texto}` → `@PreAuthorize("hasAuthority('PARTICIPAR_CHAT_OPERACAO')")`.
   - Persistência: `MensagemJpaEntity` + adapter.
   - Eventos: publica em `trader:chat:nova_mensagem:{operacaoId}`.

4. **Módulo `com.traderoperation.tempo_real`** (ponte Redis ↔ STOMP, D-25, D-26):
   - `application/port/in`: `ContadorParticipantesQuery`.
   - `application/service`: `ContadorParticipantesService` (usa Redis `SADD`/`SCARD` por operação).
   - `infrastructure/websocket/`:
     - `WebSocketConfig`: habilita broker in-memory `SimpleBroker`, endpoint `/ws/stomp` com `withSockJS().setAllowedOrigins(...)`, prefixo de app `/app`, prefixo de broker `/topic` e `/queue`.
     - `AutenticacaoHandshakeInterceptor`: antes do handshake, lê o cookie `trader_access`, valida pelo `JwtService` e põe o `UsuarioAutenticado` nos `attributes`. Rejeita 401 se não autenticar.
     - `AutenticacaoChannelInterceptor`: no `CONNECT` STOMP, carrega os direitos do usuário (via `VerificarDireitoUseCase`) e armazena no `Principal` da sessão. Em `SUBSCRIBE` para `/topic/operacoes/**`, exige `VER_OPERACAO_TEMPO_REAL`; para `/topic/chat/{id}`, exige `LER_CHAT_OPERACAO`.
     - `PresencaWebSocketListener`: escuta `SessionSubscribeEvent` / `SessionUnsubscribeEvent` / `SessionDisconnectEvent`. Em `subscribe` em `/topic/operacoes/{id}` chama `SADD`; em `unsubscribe`/`disconnect` chama `SREM`; sempre publica o novo `SCARD` em `/topic/operacoes/{id}/participantes`.
   - `infrastructure/eventos/`:
     - `OperacoesRedisListener`: assina os canais Redis e re-publica via `SimpMessagingTemplate` em `/topic/operacoes` (nova operação), `/topic/operacoes/{id}` (atualização) e `/topic/chat/{id}` (nova mensagem). Isso prepara terreno para múltiplas instâncias — mesmo com broker in-memory, cada instância recebe tudo via Redis.

5. **Migrações Flyway** (começam em V11):
   - `V11__operacoes.sql`:
     ```sql
     CREATE TABLE operacoes (
         id              UUID        PRIMARY KEY,
         ativo           VARCHAR(20) NOT NULL,
         direcao         VARCHAR(10) NOT NULL CHECK (direcao IN ('COMPRA', 'VENDA')),
         entrada         NUMERIC(18,8) NOT NULL,
         stop            NUMERIC(18,8) NOT NULL,
         alvos           NUMERIC(18,8)[] NOT NULL,
         estrategia      VARCHAR(120),
         status          VARCHAR(20) NOT NULL CHECK (status IN ('PUBLICADA', 'ENCERRADA')),
         resultado       VARCHAR(20) CHECK (resultado IN ('GAIN', 'LOSS', 'NEUTRO', 'INDEFINIDO')),
         observacao      TEXT,
         master_id       UUID NOT NULL REFERENCES usuarios(id),
         publicada_em    TIMESTAMPTZ NOT NULL,
         encerrada_em    TIMESTAMPTZ
     );
     CREATE INDEX ix_operacoes_status ON operacoes(status);
     CREATE INDEX ix_operacoes_publicada_em ON operacoes(publicada_em DESC);
     ```
   - `V12__chat_mensagens.sql`:
     ```sql
     CREATE TABLE chat_mensagens (
         id          UUID        PRIMARY KEY,
         operacao_id UUID        NOT NULL REFERENCES operacoes(id) ON DELETE CASCADE,
         autor_id    UUID        NOT NULL REFERENCES usuarios(id),
         texto       VARCHAR(500) NOT NULL,
         enviada_em  TIMESTAMPTZ NOT NULL
     );
     CREATE INDEX ix_chat_operacao_enviada ON chat_mensagens(operacao_id, enviada_em DESC);
     ```

6. **SecurityConfig**: adicionar `/ws/stomp/**` como `permitAll()` (a autenticação acontece no handshake, não via filter chain Spring Security). CSRF já ignora WebSocket por padrão.

7. **ArchUnit**: incluir `"operacoes"`, `"chat"` e `"tempo_real"` em `MODULOS`. Nova regra: `tempo_real.infrastructure.websocket` pode importar de `operacoes.application.port.in` e `chat.application.port.in`, mas nunca de `domain` ou `infrastructure` de outros módulos.

8. **Testes backend**:
   - `OperacaoServiceTest` (unit): publicar com estratégia válida, encerrar só pelo dono (403 se não), transição de status.
   - `ChatServiceTest` (unit): não envia mensagem para operação encerrada.
   - `OperacoesIntegracaoTest` (Testcontainers Postgres): master publica, cliente PRO lista ao vivo, cliente FREE sem direito → 403, FREE vê só com atraso.
   - `TempoRealIntegracaoTest` (Testcontainers Postgres + Redis `redis:7.4-alpine`): cliente PRO conecta no STOMP (via `WebSocketStompClient` do Spring Test), subscribe em `/topic/operacoes`, outra sessão (master) publica, a primeira recebe a operação em até 2s; contador de participantes vai para 1 e volta a 0 ao disconnect.
   - `ChatIntegracaoTest`: enviar mensagem via REST, outro usuário recebe pelo STOMP.

### Frontend

9. **Dependências** no `frontend-app`: `@stomp/stompjs@7.1.1` (STOMP client). Nada de SockJS no cliente — o Spring aceita WebSocket nativo quando `withSockJS()` está desabilitado; vou usar WebSocket nativo (mais simples) e remover o `.withSockJS()` do backend em dev. **Decisão de implementação**: WebSocket puro, sem fallback SockJS. Browsers modernos têm WebSocket desde 2012.

10. **Feature `operacoes/`**:
    - `api/operacoesApi.ts`: `listarAoVivo`, `obter`, `publicar`, `encerrar`.
    - `api/chatApi.ts`: `listarMensagens`, `enviar`.
    - `hooks/useOperacoesAoVivo.ts`: TanStack Query key `['operacoes', 'ao-vivo']` + invalidação quando STOMP envia nova.
    - `hooks/useOperacao.ts`: query do detalhe.
    - `hooks/useChatOperacao.ts`: histórico (REST) + append conforme STOMP recebe.
    - `hooks/usePublicarOperacao.ts`, `useEncerrarOperacao.ts` (mutations).
    - `pages/ListaOperacoesPage.tsx`: tabela/cartões das operações `PUBLICADA`, badge de participantes, botão "Publicar" só se `PUBLICAR_OPERACAO`.
    - `pages/OperacaoPage.tsx`: card com entrada/stop/alvos + badge do master + chat.
    - `componentes/FormularioPublicarOperacao.tsx`: diálogo (reusa `Dialogo`).
    - `componentes/ChatOperacao.tsx`: lista de mensagens (scroll) + input (só se `PARTICIPAR_CHAT_OPERACAO`).

11. **`shared/tempo-real/stompClient.ts`**:
    - Cria um cliente singleton por app, conectando em `ws://localhost:8090/ws/stomp` (ou wss em produção). O cookie `trader_access` vai automaticamente no handshake.
    - Hook `useAssinarTopico<T>(destino: string | null, aoReceber: (t: T) => void)`: ciclo subscribe/unsubscribe no mount/unmount; `destino: null` desativa.
    - Reconexão automática a cada 5s com backoff simples.

12. **Rotas em `App.tsx`**:
    - `/operacoes` → `RotaPorDireito direito="VER_OPERACAO_TEMPO_REAL"` (FREE cai no `/sem-permissao`; se virar demanda real, cria `RotaPorDireitoAlgum` depois).
    - `/operacoes/:id` → mesma proteção.
    - Adicionar item "Operações ao vivo" no `MenuLateral` quando o direito está presente.

13. **Testes frontend**:
    - `operacoesApi.test.ts` (vitest): `listar/publicar/encerrar` enviam o corpo certo.
    - `useChatOperacao.test.tsx`: hook anexa mensagem recebida ao histórico.
    - `ListaOperacoesPage.test.tsx`: renderiza operações, botão de publicar só com direito.

## Definition of Done (verificável)

```bash
# 1. Backend
cd nucleo-backend && ./mvnw -q verify && cd ..

# 2. Frontend
cd frontend-app && npm run typecheck && npm run lint && npm run test:ci && npm run build && cd ..

# 3. Compose completo
docker compose up -d --build
./scripts/verificar-infra.sh

# 4. Smoke tests (master publica, cliente recebe via WS)
TOKEN_CLI=... # cookie trader_access após login do cliente PRO
TOKEN_MASTER=...
curl -s -b "trader_access=$TOKEN_MASTER" -H "X-XSRF-TOKEN:..." -H "Content-Type: application/json" \
  -X POST -d '{"ativo":"NAS100","direcao":"COMPRA","entrada":20000,"stop":19900,"alvos":[20100,20200],"estrategia":"triangulo"}' \
  http://localhost:8090/api/operacoes
curl -sf -b "trader_access=$TOKEN_CLI" http://localhost:8090/api/operacoes/ao-vivo | jq 'length >= 1'

# 5. Teste manual na UI (documentado em frontend-app/README.md)
# - Master abre /operacoes, publica NAS100 compra.
# - Cliente PRO (outra aba) está em /operacoes → cartão aparece em <2s.
# - Cliente abre /operacoes/{id}, envia mensagem no chat → outros clientes veem.
# - Contador de participantes sobe/desce ao abrir/fechar aba.
# - Master clica "Encerrar" com resultado=GAIN e observação → operação sai da lista ao vivo.
# - FREE sem VER_OPERACAO_TEMPO_REAL → 403 ao abrir /operacoes.
```

Tudo deve passar. ArchUnit e os testes das skills 02 e 04 continuam verdes.

## Notas para as próximas skills

- **Skill 06 (frontend gráfico)** adiciona o Lightweight Charts dentro de `OperacaoPage` e desenha entrada/stop/alvos no gráfico.
- **Skill 07 (IA analista)** pluga `/topic/chat/{id}` para postar análise da IA junto com os humanos.
- **Skill 10 (conector MT5)** passa a receber `/topic/operacoes/*` no Copiloto do cliente para replicar no MT5 local (passando pela trava de risco).
- Encerramento automático por cruzamento de preço (vs stop/alvo) entra quando o `motor-quant` estiver recebendo cotações ao vivo do VPS (D-22 + D-27).
