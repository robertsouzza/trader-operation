# Visão e requisitos — Trader Operation

Plano completo, com diagramas: <https://claude.ai/code/artifact/5371b1fd-ed3b-4cdb-ba0e-6ee958340b49>

## Visão

Plataforma web de análise e operação em tempo real, no estilo TradingView, conectada ao MetaTrader 5, com IA embutida. O operador master (e a IA, com base no conhecimento dele e na própria base) publica operações com entrada, stop e alvos. Clientes assinantes acompanham ao vivo, conversam entre si e, se quiserem, replicam as operações no MT5 da própria máquina pelo **Copiloto IA**, que também tem uma IA explicando a operação em curso.

## Perfis

| Perfil | O que faz |
|---|---|
| Admin da plataforma | Gerencia usuários, planos, cobranças e curadoria geral |
| Operador master | Cria, valida e publica estratégias e operações; aprova estratégias da vitrine |
| Cliente | Acompanha operações, conversa no chat, usa estratégias conforme o plano, replica no MT5 |

## Planos (proposta inicial, valores a definir)

| Plano | Direitos |
|---|---|
| Free | Vê operações com atraso, chat em modo leitura |
| Pro | Operações em tempo real, chat, replicação no MT5 pelo Copiloto |
| Premium | Tudo do Pro + pedir à IA para testar estratégias próprias e publicá-las na vitrine |

## Requisitos funcionais

- **RF-01** Cadastro e login de usuários; perfil e plano atual visíveis.
- **RF-02** Assinatura de planos com pagamento recorrente.
- **RF-03** Gráfico em tempo real dos ativos (NAS100, XAUUSD no MVP) com indicadores básicos.
- **RF-04** Publicação de operação (ativo, direção, entrada, stop, alvos, estratégia de origem) pelo operador master ou pela IA.
- **RF-05** Operações aparecem ao vivo no gráfico de todos os clientes com direito pelo plano.
- **RF-06** Contador em tempo real de quantas pessoas estão em cada operação.
- **RF-07** Chat por operação entre os clientes participantes.
- **RF-08** Chat com a IA sobre o gráfico, a operação em curso e a estratégia (sem revelar as regras da estratégia).
- **RF-09** Vitrine de estratégias com métricas padronizadas (D-04).
- **RF-10** Pedido de teste de estratégia pela IA: descrição em português → regras → backtest → relatório de métricas.
- **RF-11** Fluxo de validação e aprovação da estratégia pelo operador master antes de publicar.
- **RF-12** Copiloto IA na máquina do cliente: login, recebe operações, conversa com a IA, replica no MT5 com confirmação ou automático (D-02).
- **RF-13** Motor de risco: risco máximo por operação, perda máxima diária, máximo de posições, ativos e horários permitidos, botão de pânico.
- **RF-14** Diário auditável de toda proposta, decisão e ordem (da IA, do operador e do cliente).

## Requisitos não funcionais

- **RNF-01** Sinal publicado chega à tela e ao Copiloto do cliente em até 1 segundo em condições normais.
- **RNF-02** Nenhuma senha de conta de trading de cliente é armazenada na nuvem.
- **RNF-03** Regras de estratégia nunca são enviadas ao cliente (D-03).
- **RNF-04** Toda ordem, de qualquer origem, passa pelo motor de risco; a IA não tem ferramenta para alterá-lo.
- **RNF-05** Segredos (chaves de API de IA, chaves de pagamento) criptografados e fora do repositório.
- **RNF-06** Fronteiras hexagonais verificadas por ArchUnit no build.
- **RNF-07** Interface e documentação em português (BR); código em inglês.

## Riscos conhecidos

- **Regulatório (CVM):** vender recomendações de compra/venda e operar em conta de terceiros pode exigir credenciamento; CFDs internacionais ofertados a brasileiros são zona cinzenta. Mitigação: D-02 e consulta jurídica antes de cobrar por execução automática.
- **IA erra:** indicadores calculados pelo `motor-quant`, nunca "de cabeça" pela IA; motor de risco obrigatório; diário de decisões.
- **Latência e custo de IA:** análise calculada uma vez e transmitida a todos; não é plataforma de alta frequência.
- **MT5 só em Windows:** conector roda com Python do Windows (D-13).
- **Mesas proprietárias** podem proibir copy trade; parcerias precisam respeitar as regras de cada mesa.
