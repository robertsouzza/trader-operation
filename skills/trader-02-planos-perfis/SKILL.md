---
name: trader-02-planos-perfis
description: Cria o módulo planos do nucleo-backend — enum Direito fechado no código, planos Free/Pro/Premium, matriz direito-por-plano e direito-por-perfil em memória, entidade Assinatura (origem MANUAL ou PAGAMENTO) e endpoints mínimos de admin para listar usuários e trocar plano. Direitos viram GrantedAuthority no filtro JWT para uso em @PreAuthorize. Terceira skill do roadmap; roda depois da trader-01-nucleo-core.
---

# trader-02-planos-perfis — Planos, direitos e assinaturas

## Contexto

Adiciona ao `nucleo-backend` o módulo `planos/` previsto na arquitetura. O que entra: o enum `Direito` (lista fechada), a matriz direito-por-plano e direito-por-perfil, a entidade `Assinatura` (quem tem qual plano, até quando, por qual origem) e dois endpoints de admin para listar usuários e atribuir plano manualmente. O enum `Perfil` da skill 01 não muda; a autenticação só ganha um passo extra para carregar os direitos do usuário logado como `GrantedAuthority`, de modo que `@PreAuthorize("hasAuthority('...')")` passa a funcionar em todos os módulos. Nenhum fluxo de pagamento aqui — isso é skill 09, que chamará o mesmo `AtribuirPlanoUseCase` com `origem = PAGAMENTO`.

**Assume:** skill 01 concluída (`./mvnw verify` passa, `/api/auth/me` responde 200/401, auditoria grava LOGIN).

## Referências obrigatórias

- `../trader-fullstack/SKILL.md`
- `../trader-fullstack/references/decisoes-tomadas.md` — D-10 (auth), D-11 (RBAC + hexagonal), D-14 (versões), D-15 (Direito como enum fechado), D-16 (Assinatura já na skill 02), D-17 (admin API mínima)
- `../trader-fullstack/references/visao-e-requisitos.md` — perfis, seção "Planos", RF-01, RF-14, RNF-06
- `../trader-fullstack/references/arquitetura.md` — módulo `planos/`

## Passos

1. **Pacote `com.traderoperation.planos`** sob a estrutura hexagonal já usada pelos módulos da skill 01:
   ```
   planos/
   ├── domain/
   │   ├── Plano.java                 (enum: FREE, PRO, PREMIUM)
   │   ├── Direito.java               (enum fechado — ver passo 2)
   │   ├── Assinatura.java            (record: id, usuarioId, plano, inicioEm, fimEm, status, origem)
   │   ├── StatusAssinatura.java      (enum: ATIVA, CANCELADA, VENCIDA)
   │   └── OrigemAssinatura.java      (enum: MANUAL, PAGAMENTO)
   ├── application/
   │   ├── port/in/
   │   │   ├── ConsultarPlanoDoUsuarioQuery.java
   │   │   ├── AtribuirPlanoUseCase.java
   │   │   ├── VerificarDireitoUseCase.java
   │   │   ├── ListarUsuariosQuery.java
   │   │   └── (DTOs: ResumoUsuario, PlanoAtual, CatalogoPlano)
   │   ├── port/out/
   │   │   ├── AssinaturaRepositoryPort.java
   │   │   └── UsuarioLookupPort.java   (só id, nome, email, perfil — não lê senha_hash)
   │   └── service/
   │       ├── PlanosService.java
   │       └── MatrizDeDireitos.java    (bean que combina plano+perfil → Set<Direito>)
   └── infrastructure/
       ├── adapter/in/web/
       │   ├── PlanoController.java         (/api/planos, /api/me/plano)
       │   └── AdminUsuariosController.java (/api/admin/usuarios, /api/admin/usuarios/{id}/plano)
       └── adapter/out/persistence/
           ├── AssinaturaJpaEntity.java
           ├── AssinaturaJpaRepository.java
           ├── AssinaturaPersistenceAdapter.java
           └── UsuarioLookupAdapter.java    (lê a tabela usuarios por SQL/JPQL, sem relação JPA entre módulos)
   ```
   A ligação `Usuario ↔ Assinatura` é por `usuario_id` (UUID). **Sem** `@ManyToOne` cruzando módulos: respeita D-11 (módulos só conversam por ports).

2. **Enum `Direito` (D-15)** — lista fechada, versionada no git. Nomes em português no código (coerente com o padrão do projeto: nomes de negócio em PT, código em EN só para palavras técnicas). Valores iniciais:
   ```
   VER_OPERACAO_COM_ATRASO
   VER_OPERACAO_TEMPO_REAL
   LER_CHAT_OPERACAO
   PARTICIPAR_CHAT_OPERACAO
   USAR_COPILOTO_MT5
   USAR_CHAT_IA
   PEDIR_BACKTEST_IA
   PUBLICAR_NA_VITRINE
   PUBLICAR_OPERACAO        (ligado ao perfil MASTER)
   APROVAR_ESTRATEGIA       (ligado ao perfil MASTER)
   ADMINISTRAR_USUARIOS     (ligado ao perfil ADMIN)
   ```

   `MatrizDeDireitos` tem **duas** fontes imutáveis:
   - `Map<Plano, Set<Direito>>` (plano → direitos liberados pelo plano)
   - `Map<Perfil, Set<Direito>>` (perfil → direitos liberados pelo perfil)

   `direitosDoUsuario(perfil, planoAtual)` devolve a **união** dos dois conjuntos. Direitos de perfil (ADMIN, MASTER) **não** dependem de plano: um master com plano FREE no banco continua podendo publicar operação. Cliente nunca ganha direito de MASTER/ADMIN por plano.

   Matriz padrão da skill 02:

   | Plano   | Direitos liberados |
   |---------|---|
   | FREE    | VER_OPERACAO_COM_ATRASO, LER_CHAT_OPERACAO |
   | PRO     | tudo de FREE + VER_OPERACAO_TEMPO_REAL, PARTICIPAR_CHAT_OPERACAO, USAR_COPILOTO_MT5, USAR_CHAT_IA |
   | PREMIUM | tudo de PRO + PEDIR_BACKTEST_IA, PUBLICAR_NA_VITRINE |

   | Perfil   | Direitos liberados |
   |----------|---|
   | ADMIN    | ADMINISTRAR_USUARIOS |
   | MASTER   | PUBLICAR_OPERACAO, APROVAR_ESTRATEGIA, VER_OPERACAO_TEMPO_REAL, USAR_CHAT_IA |
   | CLIENTE  | (nada por perfil; direitos vêm do plano) |

3. **Entidade `Assinatura` (D-16)** e `PlanosService`:
   - Um usuário tem **no máximo uma** assinatura `ATIVA` por vez (constraint parcial no banco, passo 7).
   - `AtribuirPlanoUseCase.atribuir(usuarioId, plano, origem, duracao)`:
     - Marca a assinatura ativa atual (se houver) como `CANCELADA` com `fim_em = now()`.
     - Cria assinatura nova `ATIVA` com `inicio_em = now()` e `fim_em = inicio_em + duracao`. Para `plano = FREE`, `duracao` pode ser `null` (não expira).
     - Audita `@Auditavel(acao = "ATRIBUIR_PLANO")`.
   - `ConsultarPlanoDoUsuarioQuery.consultar(usuarioId)` devolve a assinatura ativa; **se não houver nenhuma, retorna plano `FREE` como padrão** (todo usuário tem Free implicitamente — não precisa criar linha na migração para isso).
   - Nenhuma renovação automática por tempo nesta skill: a migração para `VENCIDA` fica para a skill 09, junto com a cobrança.

4. **Endpoints do cliente**:
   - `GET /api/planos` — público. Catálogo fixo derivado da `MatrizDeDireitos`: `[{plano, direitos: [...]}, ...]`.
   - `GET /api/me/plano` — autenticado. Retorna `{plano, inicioEm, fimEm, origem, direitos: [...]}` para o usuário logado. `direitos` já inclui a união perfil + plano.

5. **Endpoints de admin (D-17)**:
   - `GET /api/admin/usuarios?pagina=0&tamanho=20&q=<busca>` — `@PreAuthorize("hasAuthority('ADMINISTRAR_USUARIOS')")`. Retorna `{total, itens: [{id, nome, email, perfil, planoAtual, assinaturaAtivaFimEm}]}`. Paginação por `Pageable` do Spring Data; `q` filtra por `nome ILIKE` ou `email ILIKE`.
   - `PUT /api/admin/usuarios/{id}/plano` — mesma authority. Body: `{plano: "PRO", duracaoDias: 30}`. `duracaoDias` opcional (default: 30 para PRO/PREMIUM, null para FREE). Audita.
   - **Nenhuma criação ou exclusão de usuário** aqui. O admin usa o seed/futura skill de admin para isso.

6. **Direitos no filtro JWT** (modificação cirúrgica em `autenticacao/`):
   - `JwtCookieAuthFilter` passa a chamar `VerificarDireitoUseCase.direitosDoUsuario(usuarioId)` após autenticar e popula o `Authentication` com `SimpleGrantedAuthority` para cada direito (sem prefixo `ROLE_`). O perfil continua indo como authority `ROLE_<PERFIL>` para quem já usa `hasRole(...)`.
   - A dependência do filtro é na **port in** do módulo `planos` — ArchUnit continua verde. É o único ponto do `autenticacao/` que importa de `planos/`, e é via `..application.port.in..`, que a regra existente permite.
   - Cache: nenhum nesta skill. Uma consulta por requisição autenticada (poucas rps no MVP). Reavaliar na skill 05, se o tempo real crescer.

7. **Migrações Flyway** — começam em `V10__*.sql` (reserva feita pela skill 01):
   - `V10__assinaturas.sql`:
     ```sql
     CREATE TABLE assinaturas (
         id          UUID        PRIMARY KEY,
         usuario_id  UUID        NOT NULL REFERENCES usuarios(id),
         plano       VARCHAR(20) NOT NULL CHECK (plano  IN ('FREE','PRO','PREMIUM')),
         status      VARCHAR(20) NOT NULL CHECK (status IN ('ATIVA','CANCELADA','VENCIDA')),
         origem      VARCHAR(20) NOT NULL CHECK (origem IN ('MANUAL','PAGAMENTO')),
         inicio_em   TIMESTAMPTZ NOT NULL,
         fim_em      TIMESTAMPTZ NULL,
         criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
     );
     CREATE UNIQUE INDEX uk_assinaturas_ativa_por_usuario
         ON assinaturas(usuario_id) WHERE status = 'ATIVA';
     CREATE INDEX ix_assinaturas_usuario ON assinaturas(usuario_id);
     ```
   - `R__seed_dev.sql` ganha, no fim e **sem reescrever** o seed da skill 01:
     - `cliente@trader.local` → assinatura `ATIVA`, plano `PRO`, origem `MANUAL`, `fim_em = now() + interval '30 days'`.
     - `admin@trader.local` e `master@trader.local` sem assinatura (direitos vêm do perfil).
     - Aviso no topo mantido: dados sintéticos, só para dev.

8. **ArchUnit** (`ArquiteturaHexagonalTest`):
   - Adicionar `"planos"` à lista `MODULOS`. As regras existentes passam a valer para o módulo novo.
   - Adicionar regra nova: só `com.traderoperation.planos.application.service.MatrizDeDireitos` pode criar `EnumSet<Direito>` ou `Set<Direito>` como constante; outros pontos consomem a matriz pelo bean. (Evita que alguém espalhe uma segunda cópia da matriz.)
   - Regra adicional: `..autenticacao..` só pode depender de `com.traderoperation.planos.application.port.in..` dentro do pacote `planos` (sem exceção para `service/`, `domain/` ou `infrastructure/`).

9. **Auditoria (RF-14)**:
   - `AtribuirPlanoUseCase` → `@Auditavel("ATRIBUIR_PLANO")`.
   - `ListarUsuariosQuery` **não** é auditado (leitura paginada rotineira), mas qualquer `GET /api/admin/usuarios/{id}` futuro deverá ser.

10. **Testes**:
    - Unitário `MatrizDeDireitosTest`: FREE não tem `USAR_COPILOTO_MT5`; PRO tem; PREMIUM tem `PEDIR_BACKTEST_IA`; MASTER com plano FREE tem `PUBLICAR_OPERACAO`; CLIENTE com plano PREMIUM não tem `ADMINISTRAR_USUARIOS`.
    - Unitário `PlanosServiceTest`: atribuir plano cancela ativa anterior; não é possível ter duas ATIVA simultâneas (verifica a `DataIntegrityViolationException` da constraint parcial via Testcontainers quando concorrência for testada — unitário aqui só verifica a transição).
    - Integração Testcontainers (reaproveita o `AbstractIntegracaoTest` da skill 01):
      - Admin loga (CSRF + cookie) → `GET /api/admin/usuarios` responde 200 e lista pelo menos os três usuários do seed.
      - Cliente loga → `GET /api/me/plano` retorna `plano = "PRO"` (vindo do seed) e `direitos` inclui `USAR_COPILOTO_MT5`.
      - Cliente tenta `GET /api/admin/usuarios` → 403.
      - Admin faz `PUT /api/admin/usuarios/<id-do-cliente>/plano` com `{"plano":"FREE"}` → 200.
      - Cliente refaz `GET /api/me/plano` → `plano = "FREE"` e `direitos` **não** inclui `USAR_COPILOTO_MT5`.
      - `registros_auditoria` tem pelo menos um `ATRIBUIR_PLANO`.
    - ArchUnit continua passando com o módulo novo registrado.

11. **OpenAPI**: endpoints novos documentados em português no Swagger (`/swagger-ui.html`).

12. **Compose e config**: nenhuma mudança em `docker-compose.yml` nem em `application.yml` — a skill não introduz serviço novo.

## Definition of Done (verificável)

```bash
# 1. Testes (ArchUnit, unitários, integração)
cd nucleo-backend && ./mvnw verify && cd ..

# 2. Sobe infra + backend
docker compose up -d --build

# 3. Catálogo público (sem login)
curl -sf http://localhost:8090/api/planos | jq '[.[].plano]'   # ["FREE","PRO","PREMIUM"]

# 4. Login admin (com CSRF)
curl -s -c c-admin.txt http://localhost:8090/api/auth/csrf
CSRF_ADMIN=$(grep XSRF-TOKEN c-admin.txt | awk '{print $7}')
curl -s -b c-admin.txt -c c-admin.txt -H "X-XSRF-TOKEN: $CSRF_ADMIN" -H "Content-Type: application/json" \
  -d '{"email":"admin@trader.local","senha":"trader123"}' http://localhost:8090/api/auth/login

# 5. Lista admin
curl -sf -b c-admin.txt "http://localhost:8090/api/admin/usuarios?pagina=0&tamanho=20" | jq '.total >= 3'   # true
CLI_ID=$(curl -sf -b c-admin.txt "http://localhost:8090/api/admin/usuarios?q=cliente" | jq -r '.itens[0].id')

# 6. Login cliente, confere plano PRO vindo do seed
curl -s -c c-cli.txt http://localhost:8090/api/auth/csrf
CSRF_CLI=$(grep XSRF-TOKEN c-cli.txt | awk '{print $7}')
curl -s -b c-cli.txt -c c-cli.txt -H "X-XSRF-TOKEN: $CSRF_CLI" -H "Content-Type: application/json" \
  -d '{"email":"cliente@trader.local","senha":"trader123"}' http://localhost:8090/api/auth/login
curl -sf -b c-cli.txt http://localhost:8090/api/me/plano | jq '.plano'                              # "PRO"
curl -sf -b c-cli.txt http://localhost:8090/api/me/plano | jq '.direitos | index("USAR_COPILOTO_MT5") != null'  # true

# 7. Cliente barrado no endpoint admin
curl -s -o /dev/null -w "%{http_code}\n" -b c-cli.txt http://localhost:8090/api/admin/usuarios     # 403

# 8. Admin troca cliente para FREE
curl -sf -b c-admin.txt -H "X-XSRF-TOKEN: $CSRF_ADMIN" -H "Content-Type: application/json" \
  -X PUT -d '{"plano":"FREE"}' \
  "http://localhost:8090/api/admin/usuarios/$CLI_ID/plano"
curl -sf -b c-cli.txt http://localhost:8090/api/me/plano | jq '.plano'                              # "FREE"
curl -sf -b c-cli.txt http://localhost:8090/api/me/plano | jq '.direitos | index("USAR_COPILOTO_MT5")'  # null

# 9. Troca de plano foi auditada
docker compose exec -T postgres psql -U trader -d trader -tAc \
  "SELECT count(*) FROM registros_auditoria WHERE acao = 'ATRIBUIR_PLANO'"                          # >= 1

# 10. Infra base continua saudável
./scripts/verificar-infra.sh
```

Todos os itens devem passar. ArchUnit continua travando as fronteiras, agora também para o módulo `planos`.

## Pré-requisitos na máquina de dev

Nenhum novo. Java 21 e Docker já instalados na skill 00/01.

## Notas para as próximas skills

- **Skill 03 (frontend-core)** consome `GET /api/planos`, `GET /api/me/plano` e `GET /api/auth/me` para rotear por perfil + direito. A tela de login + a tela "Meu plano" nascem lá.
- **Skill 05 (operações)** usa `VerificarDireitoUseCase` para decidir se o cliente recebe operação em tempo real (`VER_OPERACAO_TEMPO_REAL`) ou com atraso (`VER_OPERACAO_COM_ATRASO`). O contador por operação depende disso.
- **Skill 07 (IA analista)** usa `USAR_CHAT_IA` e, para o fluxo de backtest, `PEDIR_BACKTEST_IA`.
- **Skill 09 (pagamentos)** chama `AtribuirPlanoUseCase` com `origem = PAGAMENTO` após confirmação do PSP. O endpoint `PUT /api/admin/usuarios/{id}/plano` **não** é reusado para pagamento.
- Migrações da skill 03 em diante começam em `V11__*.sql`.
- Quando chegar uma necessidade real de direito dinâmico (ex.: trial de 7 dias de PREMIUM para FREE), avaliar o modelo híbrido descartado em D-15 e abrir D-XX de revisão.
