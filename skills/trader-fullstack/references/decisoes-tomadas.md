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

## Em aberto (decidir antes da skill indicada)

| Tema | Decidir antes de | Opções |
|---|---|---|
| Fonte de dados de mercado para a nuvem | skill 04 | MT5 do operador master num VPS Windows alimentando a nuvem · provedor de dados pago |
| Provedor de pagamento | skill 09 | Stripe · Mercado Pago · Asaas (cobrar em real, dólar ou ambos) |
| Hospedagem de produção | skill 12 | VPS · nuvem gerenciada |
