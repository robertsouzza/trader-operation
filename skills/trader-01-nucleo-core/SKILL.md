---
name: trader-01-nucleo-core
description: Gera a base do nucleo-backend do Trader Operation — projeto Maven Spring Boot 3.5 com Java 21, monólito modular hexagonal, módulos shared (config, CORS, OpenAPI, erros, S3), autenticacao (JWT em cookie httpOnly + CSRF, perfis ADMIN/MASTER/CLIENTE) e auditoria (diário via AOP), com ArchUnit travando as fronteiras desde o dia 1. Segunda skill do roadmap; roda depois da trader-00-infra-base.
---

# trader-01-nucleo-core — Spring Boot: shared + autenticação + auditoria + ArchUnit

## Contexto

Cria o projeto Spring Boot em `nucleo-backend/`, a fundação hexagonal e os três módulos transversais de que todos os outros dependem: `shared`, `autenticacao` e `auditoria`. Nenhum módulo de negócio (planos, operações, chat etc.) entra aqui.

**Assume:** skill 00 concluída (`./scripts/verificar-infra.sh` passa: Postgres+pgvector em 5434, Redis em 6380, S3 em 8333).

## Referências obrigatórias

- `../trader-fullstack/SKILL.md`
- `../trader-fullstack/references/decisoes-tomadas.md` — D-06 (stack), D-08 (infra), D-10 (autenticação), D-11 (arquitetura), D-13 (WSL), D-14 (versões)
- `../trader-fullstack/references/arquitetura.md` — módulos do `nucleo-backend`
- `../trader-fullstack/references/visao-e-requisitos.md` — perfis, RF-01, RF-14, RNF-05, RNF-06

## Passos

1. **Projeto Maven em `nucleo-backend/`** (com `mvnw`): Spring Boot 3.5.x estável mais recente, Java 21, versões pinadas (D-14). `groupId` `com.traderoperation`, `artifactId` `nucleo-backend`. Dependências: Web, Security, Data JPA, Validation, AOP, Actuator, Data Redis, driver PostgreSQL, Flyway (`flyway-core` + `flyway-database-postgresql`), springdoc-openapi-starter-webmvc-ui, jjwt (api, impl, jackson), AWS SDK v2 `s3`, Lombok; testes com spring-boot-starter-test, spring-security-test, Testcontainers (`postgresql`, `junit-jupiter`) e ArchUnit (`archunit-junit5`).

2. **`application.yml`** com profiles `dev` (WSL, portas do `.env`), `docker` (nomes dos serviços do compose) e `test`. Porta HTTP **8090** (a 8080 está ocupada pelo backend do SGCE na máquina de dev). `spring.jpa.open-in-view=false`, `hibernate.jdbc.time_zone=UTC`, `ddl-auto=validate` (o schema é do Flyway). Segredos (JWT, S3) só por variável de ambiente, com valor de dev no profile `dev`.

3. **Pacotes** sob `com.traderoperation`, conforme `arquitetura.md`:
   ```
   com.traderoperation/
   ├── shared/
   │   ├── config/     (CorsConfig, OpenApiConfig, S3Config, RedisConfig)
   │   ├── error/      (GlobalExceptionHandler, formato único de erro em português)
   │   └── storage/    (S3StorageAdapter: put/get/delete por chave; path-style)
   ├── autenticacao/
   │   ├── domain/         (Usuario, Perfil {ADMIN, MASTER, CLIENTE}, Email)
   │   ├── application/    (port/in: LoginUseCase, RenovarSessaoUseCase, UsuarioAtualQuery;
   │   │                    port/out: UsuarioRepositoryPort; service/)
   │   └── infrastructure/
   │       ├── adapter/in/web/          (AuthController)
   │       ├── adapter/out/persistence/ (UsuarioJpaEntity, UsuarioJpaRepository, UsuarioPersistenceAdapter)
   │       └── security/                (SecurityConfig, JwtService, JwtCookieAuthFilter)
   └── auditoria/
       ├── domain/         (RegistroAuditoria, @Auditavel)
       ├── application/    (port/out: AuditoriaRepositoryPort; service/)
       └── infrastructure/
           ├── adapter/out/persistence/
           └── aspect/     (AuditoriaAspect: @Around em métodos @Auditavel)
   ```

4. **Autenticação (D-10)**:
   - `SecurityConfig`: sessão stateless; CSRF ligado com `CookieCsrfTokenRepository.withHttpOnlyFalse()` (o React lê `XSRF-TOKEN` e manda `X-XSRF-TOKEN`); CORS com origem explícita `http://localhost:5173` (Vite) e `allowCredentials=true`; `/api/auth/login`, `/api/auth/refresh`, `/api/auth/csrf`, `/actuator/health` e Swagger públicos, o resto autenticado; regras por perfil via `@PreAuthorize`.
   - `JwtService`: HS256, segredo de no mínimo 64 caracteres por variável `TRADER_JWT_SECRET`; access token 15 min, refresh 7 dias; claims `sub` (id), `perfil`.
   - `AuthController`:
     - `POST /api/auth/login` → valida e-mail e senha (BCrypt), seta cookies `trader_access` e `trader_refresh` (httpOnly, SameSite=Lax, Secure fora de dev) e devolve `{id, nome, email, perfil}` sem tokens no corpo.
     - `POST /api/auth/refresh` → lê `trader_refresh`, emite novo access.
     - `POST /api/auth/logout` → expira os dois cookies.
     - `GET /api/auth/me` → usuário atual (401 sem cookie válido).
     - `GET /api/auth/csrf` → força a emissão do cookie CSRF.
   - Erros de login devolvem mensagem genérica ("E-mail ou senha inválidos"), sem dizer qual dos dois errou.

5. **Migrações Flyway** (V1 a V9 reservadas para o core):
   - `V1__extensoes.sql`: `CREATE EXTENSION IF NOT EXISTS vector;` (garante a extensão também em produção, fora do init do Docker).
   - `V2__usuarios.sql`: tabela `usuarios` (`id uuid`, `nome`, `email` único em minúsculas, `senha_hash`, `perfil`, `ativo`, `criado_em`, `atualizado_em`).
   - `V3__auditoria.sql`: tabela `registros_auditoria` (`id`, `usuario_id`, `acao`, `entidade`, `entidade_id`, `dados jsonb`, `ocorrido_em`) com índices por `usuario_id`, `entidade` e `ocorrido_em`.
   - `R__seed_dev.sql` (só no profile `dev`, via `spring.flyway.locations`): três usuários **sintéticos** com aviso no topo do arquivo: `admin@trader.local` (ADMIN), `master@trader.local` (MASTER), `cliente@trader.local` (CLIENTE), todos com a senha `trader123`.

6. **Auditoria (RF-14)**: `@Auditavel(acao = "...")` em métodos de caso de uso; o `AuditoriaAspect` grava usuário, ação, entidade, id e um resumo em `jsonb`. Login com sucesso e falha também são auditados (sem gravar a senha).

7. **OpenAPI**: Swagger UI em `/swagger-ui.html`, com descrição em português.

8. **ArchUnit** em `src/test/java/com/traderoperation/architecture/ArquiteturaHexagonalTest.java`:
   - `..domain..` não depende de `org.springframework..`, `jakarta.persistence..` nem `com.fasterxml.jackson..`.
   - `..application..` não depende de `..infrastructure..`.
   - Um módulo só acessa outro módulo pelos pacotes `..application.port.in..` dele (exceção: `shared`).
   - Sem ciclos entre módulos.

9. **Testes**: unitários do `JwtService` e do serviço de login; integração com Testcontainers usando a mesma imagem `pgvector/pgvector:0.8.1-pg16` (migrações rodam, login seta cookie, `/me` responde 401/200, POST sem CSRF responde 403).

10. **Compose**: adicionar o serviço `nucleo-backend` ao `docker-compose.yml` com `nucleo-backend/Dockerfile.dev` (build multi-stage com Maven), porta `${TRADER_BACKEND_PORT:-8090}:8090`, profile `docker`, `depends_on` com `condition: service_healthy` em `postgres` e `redis` e `service_completed_successfully` em `s3-init`; healthcheck em `/actuator/health`. Acrescentar `TRADER_BACKEND_PORT` e `TRADER_JWT_SECRET` ao `.env.example`.

## Definition of Done (verificável)

```bash
# 1. Testes (inclui ArchUnit e Testcontainers)
cd nucleo-backend && ./mvnw verify && cd ..

# 2. Sobe infra + backend
docker compose up -d --build

# 3. Saúde e Swagger
curl -sf http://localhost:8090/actuator/health            # {"status":"UP"}
curl -sf -o /dev/null http://localhost:8090/swagger-ui.html

# 4. Sem cookie → 401
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/api/auth/me    # 401

# 5. Login seta cookies httpOnly (pegando o CSRF antes)
curl -s -c c.txt http://localhost:8090/api/auth/csrf
TOKEN=$(grep XSRF-TOKEN c.txt | awk '{print $7}')
curl -s -b c.txt -c c.txt -H "X-XSRF-TOKEN: $TOKEN" -H "Content-Type: application/json" \
  -d '{"email":"master@trader.local","senha":"trader123"}' http://localhost:8090/api/auth/login
grep -c "trader_access" c.txt                              # 1

# 6. Com cookie → 200 e perfil MASTER
curl -s -b c.txt http://localhost:8090/api/auth/me         # {"perfil":"MASTER",...}

# 7. Login auditado
docker compose exec -T postgres psql -U trader -d trader -tAc \
  "SELECT count(*) FROM registros_auditoria WHERE acao = 'LOGIN'"   # >= 1
```

Todos os itens devem passar. `./scripts/verificar-infra.sh` continua passando.

## Pré-requisito na máquina de dev

Java 21 no WSL (`java -version`). Se não houver: `sudo apt install openjdk-21-jdk`. O `mvnw` baixa o Maven sozinho.

## Notas para as próximas skills

- Skill 02 cria `planos` e liga direitos por plano ao `Usuario`; não mexe na autenticação.
- Migrações das próximas skills começam em `V10__*.sql`.
- Módulo novo = pacote novo sob `com.traderoperation`; as regras do ArchUnit já valem para ele.
- Arquivos (relatórios, anexos de estratégia) usam o `S3StorageAdapter` atrás de um port declarado no módulo que precisar.
