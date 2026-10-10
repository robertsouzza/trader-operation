# Decisões tomadas — Trader Operation

Decisões que fecharam ambiguidades do projeto. **Leia antes de gerar código.** Se algo aqui conflitar com `visao-e-requisitos.md` ou `arquitetura.md`, este arquivo vence.

Formato: cada decisão tem um código `D-XX`, a decisão, o porquê e a data. Decisões novas entram no fim da seção correspondente; decisões revistas são marcadas como **substituída por D-YY**, nunca apagadas.

---

## Produto

### D-01 · Mercado alvo

Foco em mercado internacional, principalmente **Nasdaq (NAS100) e ouro (XAUUSD)**, operados via MetaTrader 5. B3 é eventual e não entra no MVP.

**Por quê:** é o mercado que o operador master opera hoje. (03/out/2026)

### D-02 · Sinal publicado, execução pelo cliente

A plataforma **publica** a operação (entrada, stop, alvos) em tempo real. A **execução** na conta do cliente só acontece por decisão do próprio cliente: confirmando a ordem ou ligando, ele mesmo, a execução automática no Copiloto da máquina dele.

- A plataforma nunca guarda senha de conta de trading de cliente na nuvem.
- Termo de uso deixa claro que a decisão de executar é do cliente; nenhuma propaganda promete lucro.
- Antes de cobrar por execução automática, consultar advogado de mercado de capitais (ver `visao-e-requisitos.md`, seção Riscos).

**Por quê:** reduz a responsabilidade e o risco regulatório (CVM) de operar dinheiro de terceiros. (03/out/2026)

### D-03 · Estratégias ficam na nuvem

As regras das estratégias ficam só no servidor. O cliente recebe **sinais e explicações**, nunca o arquivo ou o código da estratégia.

**Por quê:** protege o trabalho do operador master e dos autores da vitrine. (03/out/2026)

### D-04 · Vitrine de estratégias

Clientes de plano superior pedem à IA para testar uma estratégia. Se validada, ela entra na vitrine com métricas padronizadas e outros clientes podem usá-la conforme o plano. O operador master também publica as suas.

Validação mínima para publicar: backtest + teste fora da amostra + período em conta demo + aprovação do operador master. Métricas obrigatórias: taxa de acerto, payoff, fator de lucro, drawdown máximo, número de operações, período testado.

**Por quê:** é o principal diferencial do produto; a validação evita estratégia "bonita no backtest" que falha ao vivo. (03/out/2026)

### D-05 · Nome do módulo do cliente

O módulo da máquina do cliente se chama **Copiloto IA**. Evitar o termo "robô" em interface, documentação e marketing. Tecnicamente o MT5 exige um programa MQL5 (EA/indicador) para desenhar ou executar no gráfico; ele é chamado de **Conector MQL5**.

**Por quê:** pedido do usuário; "robô" soa ultrapassado para um produto de IA. (03/out/2026)

---

## Stack

### D-06 · Linguagens por parte do sistema

| Parte | Stack |
|---|---|
| `nucleo-backend` | Java 21 + Spring Boot 3.5 |
| `motor-quant` | Python 3.12 + FastAPI |
| `frontend-app` | React 19 + TypeScript + Vite + React Router + TanStack Query + Vitest |
| `copiloto` | Electron + React (reaproveita componentes do `frontend-app`) |
| `conector` | Python (fork do `ariadng/metatrader-mcp-server`, licença MIT) |
| `mql5` | MQL5 (indicador que desenha entrada/stop/alvo no gráfico do MT5) |

**Por quê:** Java/Spring e React são a base que o usuário já domina (projetos SGCE e OmniCore). Python é o padrão do mercado quantitativo (pandas, numpy, vectorbt) e dos SDKs de IA, e o pacote oficial do MetaTrader5 é Python. (03/out/2026)

### D-07 · Gráficos

TradingView **Lightweight Charts** (Apache 2.0) no MVP. A Charting Library completa da TradingView exige pedido de licença e fica para quando houver clientes.

### D-08 · Infraestrutura

PostgreSQL 16 com extensão **pgvector** (memória/conhecimento da IA), Flyway para migrações, Redis (pub/sub de tempo real e contadores), armazenamento compatível com S3 para arquivos.

Em dev o S3 é o **SeaweedFS** (`chrislusf/seaweedfs`, Apache 2.0), não o MinIO: em out/2026 as imagens oficiais do MinIO não estão mais disponíveis no Docker Hub. O código acessa o armazenamento só pela API S3 (AWS SDK), então trocar por S3 da AWS, R2 ou outro em produção é só configuração. (03/out/2026)

### D-09 · Tempo real

Redis pub/sub + WebSocket/STOMP no `nucleo-backend` para sinais, contador de pessoas por operação e chat. Mesmo padrão do SGCE.

### D-10 · Autenticação

JWT em **cookie httpOnly + Secure + SameSite=Lax**, CSRF em mutações, CORS com origem explícita. Nunca guardar token em `localStorage`. Mesmo padrão do SGCE (D-08 de lá).

### D-11 · Arquitetura do backend

Monólito modular hexagonal (`domain`, `application/port`, `infrastructure/adapter`) com **ArchUnit** travando as fronteiras desde a primeira skill. Pagamentos via adapters por provedor (padrão multi-PSP do OmniCore). Permissões via matriz de perfis + direitos por plano (padrão RBAC do OmniCore).

### D-12 · IA plugável

O `motor-quant` fala com uma interface única de provedor de IA. Claude (Anthropic) é o provedor padrão; OpenAI e modelo local (Ollama) entram como adapters. A IA nunca acessa o MT5 diretamente: usa ferramentas controladas (`ler_cotacoes`, `calcular_indicador`, `buscar_conhecimento`, `propor_ordem` etc.), e toda ordem passa pelo motor de risco.

### D-13 · Ambiente de desenvolvimento

Repositório em `~/trader-operation` no **WSL2**. `nucleo-backend`, `motor-quant` e `frontend-app` rodam no WSL. O `conector` precisa rodar com o **Python do Windows** (o pacote MetaTrader5 não existe para Linux); o código fica no mesmo repositório e a comunicação é por `localhost` (recomendado `networkingMode=mirrored` no `.wslconfig`).

### D-14 · Versões

Usar sempre a versão estável mais recente compatível de cada dependência e **pinar** no arquivo de build (`pom.xml`, `package.json`, `pyproject.toml`). Imagens Docker com tag explícita, nunca `:latest`.

---

## Planos e perfis

### D-15 · Direito é enum fechado no código

A lista de direitos (`VER_OPERACAO_TEMPO_REAL`, `USAR_COPILOTO_MT5`, `PEDIR_BACKTEST_IA` etc.) é um `enum Direito` em `com.traderoperation.planos.domain`, versionado no git. As matrizes **plano → direitos** e **perfil → direitos** vivem como `Map` imutável no bean `MatrizDeDireitos`, também versionado. O conjunto de direitos de um usuário é a união das duas matrizes (perfil + plano ativo).

Trocar a matriz exige deploy. Nenhuma edição por admin em tempo de execução nesta fase. Se um direito dinâmico virar necessidade real (ex.: trial de 7 dias de PREMIUM), avaliar o modelo híbrido (enum + tabela de associação) numa decisão de revisão.

**Por quê:** simples, testável, impossível de ficar inconsistente, e evita UI de admin para editar permissões antes do produto existir. (09/out/2026)

### D-16 · Assinatura é modelada já na skill 02

A entidade `Assinatura(usuario_id, plano, inicio_em, fim_em, status, origem)` nasce na skill 02, com `status ∈ {ATIVA, CANCELADA, VENCIDA}` e `origem ∈ {MANUAL, PAGAMENTO}`. Um usuário tem no máximo uma assinatura `ATIVA` (constraint parcial no banco). Plano atual = assinatura ativa; sem assinatura ativa, o padrão é `FREE`.

A skill 09 (pagamentos) não cria tabela nova: só chama `AtribuirPlanoUseCase` com `origem = PAGAMENTO` depois da confirmação do PSP, reusando o mesmo caminho do admin.

**Por quê:** evita migração dupla depois e deixa o modelo coerente desde o início; a complexidade adicional é pequena (uma tabela, dois enums). (09/out/2026)

### D-17 · Admin API mínima na skill 02

A skill 02 expõe só dois endpoints de admin: `GET /api/admin/usuarios` (paginado, com busca) e `PUT /api/admin/usuarios/{id}/plano` (cria assinatura manual). Nenhum CRUD de usuário, nenhum endpoint para editar perfil ou ativar/desativar nesta skill. Essas operações ficam para uma skill de admin futura, quando houver tela.

**Por quê:** cobre o que a vitrine e os pagamentos vão precisar (atribuir plano a cliente) sem inflar o escopo. Até lá, admin cria usuários novos pelo seed ou direto no banco. (09/out/2026)

---

## Frontend

### D-19 · HTTP via fetch + wrapper fino

O `frontend-app` conversa com o backend por `fetch` nativo encapsulado num helper (`shared/api/apiClient.ts`). Sem axios, sem ky. O wrapper sempre inclui `credentials: 'include'`, chama `GET /api/auth/csrf` sob demanda, injeta `X-XSRF-TOKEN` em mutações e normaliza erros em `NaoAutorizadoError` / `AcessoNegadoError` / `ErroApi`. Nunca lê nem grava token em `localStorage`.

**Por quê:** zero dependência extra, casa bem com TanStack Query e deixa a camada de rede visível e testável. (09/out/2026)

### D-20 · Tailwind CSS, sem lib de componentes

Estilização via Tailwind CSS 4 (plugin `@tailwindcss/vite`). Botão, campo, diálogo e etc. nascem em `src/shared/componentes/` escritos à mão com classes Tailwind. Nada de shadcn/ui, Chakra ou MUI nesta fase; se um componente complexo (data picker, combobox) virar necessidade real, reavalia com decisão nova.

**Por quê:** produtividade do utility-first sem o peso de uma lib que impõe seu próprio design system antes de a UI do produto existir. (09/out/2026)

### D-21 · Estrutura feature-based espelhando o backend

Dentro de `frontend-app/src/`: `shared/` (api, layout, componentes, segurança) e `features/{autenticacao, planos, admin, home}`. Cada feature tem seus próprios `api/`, `hooks/`, `pages/` e `componentes/`. Nenhum `components/`, `services/` ou `pages/` globais fora de `shared/`.

**Por quê:** espelha os módulos hexagonais do `nucleo-backend`; mover uma feature inteira é só mover a pasta; a cabeça do leitor segue o mesmo mapa dos dois lados. (09/out/2026)

---

## Motor quantitativo

### D-22 · Fonte de dados = MT5 do master no VPS Windows

Em produção, o `motor-quant` recebe candles de NAS100 e XAUUSD do MT5 do operador master rodando num VPS Windows (via o `conector` da skill 10). O conector empurra lotes com `POST /api/motor/ingest/candles` autenticado pelo shared secret interno (D-23). Em dev, o `motor-quant` carrega CSVs sintéticos no startup quando `TRADER_MOTOR_SEED=dev`. Provedor de dados pago fica para depois do MVP, se os clientes exigirem independência do VPS.

**Por quê:** reusa a conta de trading que o master já tem (custo zero), dados reais do mesmo broker, latency aceitável para o tipo de operação do produto. (10/out/2026)

### D-23 · Shared secret interno no motor-quant

Toda rota de negócio do `motor-quant` exige header `X-INTERNAL-TOKEN` comparado por `secrets.compare_digest` com `MOTOR_SHARED_TOKEN` do ambiente. Só `/healthz` é público. Mesmo que o container viva na rede interna do compose, o header evita que erro de configuração (uma porta exposta por engano, um túnel aberto para debug) vire exposição pública. Autenticação de usuário final nunca chega ao `motor-quant` — quem é o usuário é responsabilidade do `nucleo-backend`.

**Por quê:** defesa em profundidade quase sem custo, e deixa pronto para o dia em que o motor rode num host separado da nuvem. (10/out/2026)

### D-24 · Indicadores iniciais: SMA, EMA, RSI, MACD

A skill 04 entrega quatro indicadores sobre fechamentos: **SMA**, **EMA**, **RSI** (14 por padrão) e **MACD** (12/26/9 por padrão, com linha de sinal e histograma). Todos como funções puras sobre `numpy.ndarray` em `motor_quant/dominio/indicadores.py`, sem I/O. Bandas de Bollinger, Stochastic e outros entram quando houver demanda real das estratégias da vitrine.

**Por quê:** cobre o que o operador master já usa hoje e é suficiente para a IA explicar e para o backtest da skill 08 começar. Lista mínima evita que a skill 04 vire um `ta-lib` reimplementado antes de a plataforma rodar. (10/out/2026)

---

## Fluxo de trabalho

### D-18 · Fim de skill = commit + push + PR automáticos

Quando uma skill fecha com Definition of Done verde, no mesmo passo que atualiza `README.md` ("Estado atual") e `CLAUDE.md` ("Onde paramos"), o Claude Code **comita, dá push e abre PR** da branch `trader-XX-nome` para `main` sem precisar que o Roberto peça. Rascunho/spec intermediária (ex.: só o `SKILL.md`) pode subir sem PR; o PR só nasce com o DoD verde. Nunca força push, nunca mescla sozinho — o merge é do Roberto.

**Por quê:** elimina o passo manual repetitivo de "sobe pra main" sem perder o portão humano no merge. (09/out/2026)

---

## Em aberto (decidir antes da skill indicada)

| Tema | Decidir antes de | Opções |
|---|---|---|
| Fonte de dados de mercado para a nuvem | skill 04 | MT5 do operador master num VPS Windows alimentando a nuvem · provedor de dados pago |
| Provedor de pagamento | skill 09 | Stripe · Mercado Pago · Asaas (cobrar em real, dólar ou ambos) |
| Hospedagem de produção | skill 12 | VPS · nuvem gerenciada |
