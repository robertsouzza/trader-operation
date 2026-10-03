# Arquitetura — Trader Operation

## Visão geral

```
                 ┌──────────────────────────── Nuvem ────────────────────────────┐
 Navegador  ───► │ frontend-app (React)                                           │
 (cliente,       │      │ HTTPS + WebSocket/STOMP                                 │
  master)        │      ▼                                                         │
                 │ nucleo-backend (Spring Boot) ◄──► PostgreSQL + pgvector        │
                 │      │   ▲                     ◄──► Redis (pub/sub, contadores)│
                 │      │   │ REST interno        ◄──► S3 (SeaweedFS em dev)      │
                 │      ▼   │                                                     │
                 │ motor-quant (FastAPI) ──► Provedores de IA (Claude, OpenAI...) │
                 └──────────────────────────────▲────────────────────────────────┘
                                                │ HTTPS + WebSocket
                 ┌──────────── Máquina do cliente (Windows) ─────────────┐
                 │ copiloto (Electron) ──► conector (Python) ──► MT5      │
                 │                                   mql5 (indicador) ◄──┘│
                 └────────────────────────────────────────────────────────┘
```

## Responsabilidades

| Parte | Faz | Não faz |
|---|---|---|
| `nucleo-backend` | Usuários, perfis, planos e direitos, pagamentos, operações publicadas, tempo real, chat, vitrine, motor de risco, diário | Cálculo de indicador, backtest, chamada direta a IA |
| `motor-quant` | Dados de mercado, indicadores, backtest, orquestração da IA e ferramentas da IA | Autenticação de usuário final, cobrança |
| `frontend-app` | Telas web: login, planos, gráfico ao vivo, operações, chats, vitrine, painel do master | Regra de negócio |
| `copiloto` | Login do cliente, recebe operações, chat com IA, aciona o conector | Guardar regras de estratégia |
| `conector` | Lê conta/posições e executa ordens no MT5 local, atrás da trava de risco local | Decidir operações |
| `mql5` | Desenhar entrada/stop/alvo no gráfico do MT5 | Executar sem passar pelo conector |

## Módulos do `nucleo-backend` (pacote raiz `com.traderoperation`)

```
com.traderoperation/
├── shared/        (config, segurança comum, storage, erros)
├── autenticacao/  (usuário, login, cookie JWT, CSRF)
├── auditoria/     (diário de eventos)
├── planos/        (planos, direitos, assinaturas)
├── pagamentos/    (adapters por provedor)
├── operacoes/     (operação publicada, participantes, contador)
├── chat/          (mensagens por operação)
├── estrategias/   (vitrine, pedidos de teste, validação, métricas)
├── risco/         (regras e validação de ordens)
└── tempo_real/    (Redis pub/sub + STOMP)
```

Cada módulo segue `domain/`, `application/port/{in,out}`, `application/service/`, `infrastructure/adapter/{in/web,out/persistence}`. ArchUnit garante: `domain` não depende de Spring nem de `infrastructure`; módulos só conversam por ports.
