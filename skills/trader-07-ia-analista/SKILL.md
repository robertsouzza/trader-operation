---
name: trader-07-ia-analista
description: Chat com a IA analista integrado ao gráfico. motor-quant ganha uma camada de IA plugável (Anthropic Claude padrão, OpenAI/Ollama como adapters futuros) com uma ferramenta `ler_cotacoes` que delega ao próprio motor; nucleo-backend ganha o módulo `ia` que proxyifica o motor (sem vazar segredo), persiste cada sessão/mensagem em Postgres e faz streaming via STOMP para a UI; frontend ganha a feature `ia/` com um painel de chat IA ao lado do chat de clientes dentro da OperacaoPage. Sétima skill do roadmap; roda depois da trader-06-frontend-grafico.
---

# trader-07-ia-analista — IA analista com tool-use e streaming

## Contexto

Dá à plataforma a feature de IA (RF-08): dentro do detalhe da operação, o cliente abre um painel "IA analista" ao lado do chat humano e conversa com Claude Sonnet 5.5. A IA pode chamar a ferramenta `ler_cotacoes` para pegar os candles do ativo via motor-quant (os mesmos que o gráfico usa) e explicar movimentações em contexto. Nenhuma ferramenta adicional entra nesta skill — expansão (`calcular_indicador`, `consultar_operacao_atual`, `buscar_conhecimento`) fica para skills futuras, com código em cima do mesmo loop de tool-use.

A resposta chega aos poucos via STOMP (reusa a infra da skill 05), e cada mensagem (usuário e IA) é persistida em Postgres (RF-14, D-33). O browser **nunca** vê a chave da Anthropic — ela vive só no `motor-quant` (mesma estratégia do shared secret, D-23).

**Assume:** skill 06 concluída. Backend tem `/api/mercado/candles` proxyficando o motor; motor-quant está rodando com seed sintético ou candles do VPS (D-22); frontend tem `OperacaoPage` com gráfico, painel lateral e chat STOMP.

## Referências obrigatórias

- `../trader-fullstack/SKILL.md`
- `../trader-fullstack/references/decisoes-tomadas.md` — D-10 (auth), D-12 (IA plugável), D-14 (versões), D-15 (direitos), D-22 (fonte de dados), D-23 (shared secret), D-25 (STOMP), D-26 (Postgres + Redis), D-28 (proxy sem vazar segredo), D-31 (Sonnet 5.5), D-32 (streaming via STOMP), D-33 (histórico em Postgres), D-34 (tools iniciais = `ler_cotacoes`)
- `../trader-fullstack/references/visao-e-requisitos.md` — RF-08 (chat IA), RF-10 (futuro backtest IA), riscos "IA erra" e "custo de IA"
- `../trader-fullstack/references/arquitetura.md` — `motor-quant` como orquestrador da IA; `nucleo-backend` como quem fala com o usuário final

## Passos

### Motor-quant — adapter de IA + loop de tool-use

1. **Nova dependência** em `motor-quant/pyproject.toml`: `anthropic==0.68.0` (SDK oficial, Python, sync + async). Pinada (D-14).

2. **Pacote `motor_quant.ia`** (hexagonal lite):
   - `dominio/mensagem.py`: `Papel = Literal["user", "assistant", "tool"]`, `Mensagem` (BaseModel: `papel`, `texto`, `nome_ferramenta` opcional, `argumentos_ferramenta` opcional dict, `resultado_ferramenta` opcional str).
   - `aplicacao/porta_provedor.py`: `ProvedorIAPort` (ABC). Método único `conversar(mensagens: list[Mensagem], ferramentas: list[DefinicaoFerramenta]) -> AsyncIterator[FragmentoResposta]`. `FragmentoResposta` é um union: `TrechoTexto(texto)`, `ChamadaDeFerramenta(id, nome, argumentos)`, `FimDeTurno(parar_motivo)`.
   - `aplicacao/porta_ferramenta.py`: `FerramentaPort` com `nome: str`, `descricao: str`, `schema: dict` (JSON Schema), `async executar(args: dict) -> str` (devolve o JSON do resultado como string, para a IA ler).
   - `aplicacao/servico.py`: `ChatIAServico` orquestra o loop:
     ```
     ciclo:
       chama provedor.conversar(mensagens, ferramentas)
       para cada fragmento:
         se TrechoTexto: emite para quem está consumindo
         se ChamadaDeFerramenta: executa a ferramenta, adiciona resultado às mensagens
         se FimDeTurno(parar_motivo="end_turn"): sai
         se FimDeTurno(parar_motivo="tool_use"): volta no while com as mensagens atualizadas
       limite máximo 5 iterações por turno (anti-loop infinito)
     ```
     Retorna `AsyncIterator[FragmentoResposta]` para o endpoint fazer streaming.
   - `infraestrutura/provedor/claude_provedor.py`: implementa `ProvedorIAPort` via SDK `anthropic.AsyncAnthropic`. Modelo padrão `claude-sonnet-5-5` (D-31). System prompt curto em pt-BR explicando: "Você é um analista técnico; use a ferramenta `ler_cotacoes` para olhar o gráfico; **nunca invente preços**; cite sempre o timestamp dos candles que leu".
   - `infraestrutura/ferramentas/ler_cotacoes.py`: `FerramentaPort` que chama o próprio `CandleRepositorio` do motor (reusa infra da skill 04). Schema: `{ativo: Ativo, timeframe: Timeframe, limite: int=200}`. Devolve JSON serializado dos candles.

3. **Config nova** em `motor_quant.config.Settings`:
   - `anthropic_api_key: str` (obrigatório; sem default inseguro). Lido de `ANTHROPIC_API_KEY`.
   - `modelo_padrao: str = "claude-sonnet-5-5"`.
   - `max_tokens_resposta: int = 2048`.
   - `max_iteracoes_tool_use: int = 5`.

4. **Novo endpoint** em `motor_quant.web.rotas.ia`:
   - `POST /api/motor/ia/chat` — Server-Sent Events com `text/event-stream`.
     - Body: `{mensagens: [Mensagem], ferramentas_permitidas: ["ler_cotacoes"]}`.
     - Resposta: eventos SSE com `data: {tipo, ...}` por fragmento. Tipos: `trecho`, `ferramenta`, `fim`.
     - Dependência: `exigir_token_interno` (D-23). Browser nunca chega aqui — só o nucleo-backend proxyifica.

5. **Testes motor-quant** (`tests/`):
   - `test_ler_cotacoes.py`: ferramenta executa contra repositório em memória; schema valida argumentos.
   - `test_chat_ia_servico.py`: provedor fake que emite `TrechoTexto` + `ChamadaDeFerramenta(ler_cotacoes)` + `FimDeTurno(tool_use)` + `TrechoTexto` + `FimDeTurno(end_turn)` → serviço executa a ferramenta de verdade, retoma a chamada com o resultado e emite todos os fragmentos.
   - `test_api_ia_chat.py`: endpoint sem token → 401; com token certo e provedor fake stubado via dependency override → stream de SSE válido; limite de 5 iterações respeitado.
   - `test_claude_provedor.py` *opcional*: usa `respx` para mockar a API da Anthropic; só se o investimento valer. Senão, o adapter real fica fora dos testes automatizados (confiando no SDK).

6. **Nenhuma persistência no motor-quant** — ele é stateless. A persistência das mensagens fica no nucleo-backend.

### Nucleo-backend — módulo `ia` com persistência e ponte STOMP

7. **Dependência nova** no `nucleo-backend/pom.xml`: `spring-boot-starter-webflux` (só para o `WebClient` reativo que consome o SSE do motor-quant; mais fácil que SSE síncrono com `RestClient`).

8. **Config** em `TraderProperties.Motor` (já tem) — reusa `baseUrl` e `sharedToken`. Nada novo.

9. **Módulo `com.traderoperation.ia`** (hexagonal):
   - `domain/`:
     - `SessaoIA` (record: `id`, `usuarioId`, `operacaoId`, `criadaEm`).
     - `MensagemIA` (record: `id`, `sessaoId`, `papel` [`USUARIO`/`IA`/`FERRAMENTA`], `texto`, `nomeFerramenta?`, `argumentosJson?`, `resultadoJson?`, `enviadaEm`).
   - `application/port/in`:
     - `IniciarSessaoUseCase.iniciar(usuarioId, operacaoId) -> SessaoIA` (idempotente: se já existe sessão para esse par, devolve).
     - `EnviarMensagemIAUseCase.enviar(sessaoId, usuarioId, texto) -> Flux<FragmentoResposta>` (streaming).
     - `ListarMensagensIAQuery.listar(sessaoId) -> List<MensagemIA>`.
   - `application/port/out`:
     - `SessaoIARepositoryPort`, `MensagemIARepositoryPort`.
     - `MotorIAPort.conversar(mensagens: List<MensagemIA>) -> Flux<FragmentoResposta>`.
   - `application/service/ChatIAService`:
     - Carrega histórico da sessão, acrescenta a nova mensagem do usuário, chama `MotorIAPort.conversar(...)`, acumula o texto da IA fragmento a fragmento, no fim grava `MensagemIA(IA, textoCompleto)`.
     - `@Auditavel("CHAT_IA")`.
     - Verifica direito `USAR_CHAT_IA` do usuário (via `VerificarDireitoUseCase`).
   - `infrastructure/adapter/in/web/IAController`:
     - `POST /api/ia/sessoes {operacaoId}` → cria/retorna sessão.
     - `GET /api/ia/sessoes/{sessaoId}/mensagens` → histórico (REST).
     - `POST /api/ia/sessoes/{sessaoId}/mensagens {texto}` → **não** devolve o corpo da IA; só persiste a do usuário e dispara o streaming via STOMP. Devolve 202 com `id` da mensagem do usuário.
     - Todos exigem autenticação por cookie e direito `USAR_CHAT_IA`.
   - `infrastructure/adapter/out/motor/MotorIAHttpAdapter`:
     - `WebClient` com base URL do motor + header `X-INTERNAL-TOKEN`.
     - Faz `POST /api/motor/ia/chat` como SSE, converte eventos em `Flux<FragmentoResposta>`.
   - `infrastructure/adapter/out/persistence/`:
     - `SessaoIAJpaEntity`, `MensagemIAJpaEntity`, repos e adapters.
   - `infrastructure/adapter/out/eventos/StompIAStreamer`:
     - Recebe o `Flux<FragmentoResposta>` do service e, para cada fragmento, publica em `/topic/ia/sessao/{sessaoId}` via `SimpMessagingTemplate`. No fim, publica `{tipo:"fim"}`.

10. **Migrações Flyway** (começam em V13):
    - `V13__ia.sql`:
      ```sql
      CREATE TABLE ia_sessoes (
          id            UUID PRIMARY KEY,
          usuario_id    UUID NOT NULL REFERENCES usuarios(id),
          operacao_id   UUID REFERENCES operacoes(id) ON DELETE CASCADE,
          criada_em     TIMESTAMPTZ NOT NULL,
          CONSTRAINT uk_ia_sessao_usuario_operacao UNIQUE (usuario_id, operacao_id)
      );
      CREATE TABLE ia_mensagens (
          id              UUID PRIMARY KEY,
          sessao_id       UUID NOT NULL REFERENCES ia_sessoes(id) ON DELETE CASCADE,
          papel           VARCHAR(20) NOT NULL CHECK (papel IN ('USUARIO','IA','FERRAMENTA')),
          texto           TEXT NOT NULL,
          nome_ferramenta VARCHAR(80),
          argumentos      JSONB,
          resultado       JSONB,
          enviada_em      TIMESTAMPTZ NOT NULL
      );
      CREATE INDEX ix_ia_mensagens_sessao_envio ON ia_mensagens(sessao_id, enviada_em);
      ```

11. **Canal STOMP novo**:
    - `/topic/ia/sessao/{sessaoId}` — streaming da resposta.
    - `AutenticacaoChannelInterceptor` do módulo `tempo_real` ganha regra: `SUBSCRIBE` em `/topic/ia/sessao/{sessaoId}` só é permitido se o usuário logado for o dono da sessão (`SessaoIAQuery.perteneceAo(sessaoId, usuarioId)`) **e** tiver `USAR_CHAT_IA`.

12. **ArchUnit** inclui `"ia"` nos módulos. `tempo_real` passa a conhecer uma port in de `ia` para verificar propriedade da sessão.

13. **Testes backend**:
    - `ChatIAServiceTest` com `MotorIAPort` fake: persiste USUARIO e IA; a IA é a concatenação dos fragmentos; direito ausente → `AccessDeniedException`.
    - `IAControllerTest`: 401/403/202 nos endpoints certos.
    - `IAIntegracaoTest` com `MotorIAPort` mockado via `@MockitoBean`: cliente PRO cria sessão, envia mensagem, histórico aparece.

### Frontend — feature `ia/` no painel lateral

14. **Feature `src/features/ia/`**:
    - `api/iaApi.ts`: `criarSessao(operacaoId)`, `enviarMensagem(sessaoId, texto)`, `listarMensagens(sessaoId)`.
    - `hooks/useSessaoIA.ts`: cria sessão sob demanda, cache por `operacaoId`.
    - `hooks/useChatIA.ts`: histórico (REST) + assinatura STOMP `/topic/ia/sessao/{sessaoId}` para fragmentos. Mantém um estado local `respostaEmProgresso: string` que concatena `TrechoTexto`; quando chega `{tipo:"fim"}`, consolida como mensagem IA no cache do React Query.
    - `componentes/ChatIA.tsx`: parecido com `ChatOperacao`, mas com fundo levemente diferente (gradiente acento sutil) e indicador "IA digitando..." enquanto a resposta progride.
    - `OperacaoPage` ganha abas no painel lateral (`Clientes` / `IA analista`); a aba "IA analista" só é visível se `direitos.has('USAR_CHAT_IA')`.

15. **Testes frontend**:
    - `iaApi.test.ts`: endpoints certos.
    - `useChatIA.test.tsx`: fragmentos chegam, mensagem IA é consolidada corretamente.
    - `ChatIA.test.tsx`: renderiza histórico e esconde input sem direito.

### Config, segredos e compose

16. **Chave da Anthropic**:
    - `.env.example` ganha `ANTHROPIC_API_KEY=` (vazio por padrão).
    - `docker-compose.yml`: serviço `motor-quant` recebe a env; se vazia, o motor loga aviso no startup e **falha preguiçosamente** só quando alguém chamar `/api/motor/ia/chat`. Isso permite rodar o resto do stack sem a chave.
    - `README.md` da raiz e `motor-quant/README.md` documentam como pegar a chave em console.anthropic.com.

## Definition of Done (verificável)

```bash
# Motor-quant
cd motor-quant && . .venv/bin/activate && pytest && ruff check src tests && mypy --strict src && deactivate && cd ..

# Backend
cd nucleo-backend && ./mvnw test && cd ..

# Frontend
cd frontend-app && npm run typecheck && npm run lint && npm run test:ci && npm run build && cd ..

# Compose completo com a chave real
export ANTHROPIC_API_KEY=sk-ant-...
docker compose up -d --build

# Smoke: login cliente PRO, cria sessão, envia pergunta
curl -s -c c.txt http://localhost:8090/api/auth/csrf > /dev/null
TK=$(grep XSRF-TOKEN c.txt | awk '{print $7}')
curl -s -b c.txt -c c.txt -H "X-XSRF-TOKEN:$TK" -H "Content-Type: application/json" \
  -d '{"email":"cliente@trader.local","senha":"trader123"}' http://localhost:8090/api/auth/login > /dev/null

# Pega uma operação publicada pelo master (ou cria uma)
OP_ID=... # id de uma operação PUBLICADA

# Cria sessão
SESSAO=$(curl -sf -b c.txt -c c.txt -H "X-XSRF-TOKEN:$TK" -H "Content-Type: application/json" \
  -X POST -d "{\"operacaoId\":\"$OP_ID\"}" http://localhost:8090/api/ia/sessoes \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['id'])")

# Envia pergunta — o streaming chega pelo STOMP; aqui só confirmamos o 202
curl -s -o /dev/null -w "%{http_code}\n" -b c.txt -H "X-XSRF-TOKEN:$TK" -H "Content-Type: application/json" \
  -X POST -d '{"texto":"O que você vê no gráfico desta operação?"}' \
  "http://localhost:8090/api/ia/sessoes/$SESSAO/mensagens"
# Esperado: 202

# Após ~5-15s, histórico deve ter: USUARIO, FERRAMENTA (ler_cotacoes) e IA
curl -sf -b c.txt "http://localhost:8090/api/ia/sessoes/$SESSAO/mensagens" \
  | python3 -c "import json,sys;ms=json.load(sys.stdin);print('mensagens:',len(ms));print('papeis:',[m['papel'] for m in ms])"
```

Teste manual na UI: abrir `/operacoes/:id`, clicar na aba "IA analista", perguntar "O que você vê neste gráfico?" — resposta deve chegar aos poucos (streaming), citando candles reais.

## Pré-requisitos na máquina de dev

- `ANTHROPIC_API_KEY` no `.env` (criar em console.anthropic.com; cuidado para não commitar).
- Nenhuma mudança de infra; Postgres, Redis e motor-quant já existem.

## Notas para as próximas skills

- **Skill 08 (estratégias/backtest)** adiciona as ferramentas `pedir_backtest`, `publicar_estrategia` (com aprovação do master) e integra `vectorbt` no motor-quant.
- **Skill 11 (Copiloto Electron)** reusa `ChatIA` no app do cliente, assinando o mesmo canal STOMP.
- Adapter de OpenAI entra como novo `ProvedorIAPort` quando houver demanda; Ollama idem para rodar local (D-12).
- Rate limiting por usuário/dia (controle de custo) entra numa decisão nova quando o produto tiver assinantes de verdade.
- RAG (`buscar_conhecimento`) usando pgvector entra quando o master começar a cadastrar material de referência.
