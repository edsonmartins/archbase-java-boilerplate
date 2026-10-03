# 14. Infraestrutura do Boilerplate

Padrões reais do módulo `archbase-boilerplate-rest` (e das exceções do `core`) que o restante da
skill não cobre: tratamento de erros, multi-tenancy, seeds, configuração, migrations e testes.

Pacote base: `br.com.archbase.boilerplate.rest` (exceções de domínio em
`br.com.archbase.boilerplate.core.domain.exception`).

---

## Hierarquia de exceções (core)

**Pacote**: `core/domain/exception`. Todas estendem `RuntimeException` via `BoilerplateException`.

| Classe | Construtor | HTTP (via `RestExceptionHandler`) |
|--------|-----------|------|
| `BoilerplateException` | `(String message)`, `(String message, Throwable cause)` | 400 |
| `EntityNotFoundException` | `(String entityName, String identifier)` ou `(entityName, fieldName, identifier)` | 404 |
| `DuplicateEntityException` | `(String entityName, String field, String value)` | 409 |
| `BusinessValidationException` | `(String message)` ou `(String field, String message)` | 422 |

```java
throw new EntityNotFoundException("Produto", id);               // "Produto não encontrado(a): <id>"
throw new DuplicateEntityException("Produto", "SKU", sku);      // "Produto já existe com SKU: <sku>"
throw new BusinessValidationException("estoque", "Estoque insuficiente");
```

Regra de negócio violada = lance uma subclasse de `BoilerplateException`. Não monte `ResponseEntity`
de erro no service nem no controller. Observação: `Produto.removerEstoque` (domínio) lança
`IllegalArgumentException`, que o handler converte em 400.

---

## RestExceptionHandler, ApiError, ApiSubError, ApiValidationError

**Pacote**: `rest.infrastructure.error`.

`RestExceptionHandler` é `@Order(Ordered.HIGHEST_PRECEDENCE) @ControllerAdvice` e estende
`ResponseEntityExceptionHandler`. Todo erro sai como `ApiError`:

```java
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private HttpStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime timestamp;
    private String message;
    private String debugMessage;
    private String path;
    private List<ApiSubError> subErrors;
    // construtores: (status), (status, Throwable), (status, message), (status, message, Throwable)
}

public interface ApiSubError { }                       // marcador

@Data @Builder @AllArgsConstructor @EqualsAndHashCode(callSuper = false)
public class ApiValidationError implements ApiSubError {
    private String object;
    private String field;
    private Object rejectedValue;
    private String message;
}
```

`ApiError` tem helpers `addValidationFieldErrors(List<FieldError>)`,
`addValidationObjectErrors(List<ObjectError>)` e `addValidationErrors(Set<ConstraintViolation<?>>)`
(usa só `jakarta.validation.Path`, sem classe `internal` do Hibernate Validator).

Mapeamento atual:

| Exceção | Status | Mensagem |
|---------|--------|----------|
| `MethodArgumentNotValidException` (`@Valid`) | 400 | "Erro de validação" + `subErrors` |
| `ConstraintViolationException` | 400 | "Erro de validação" + `subErrors` |
| `HttpMessageNotReadableException` | 400 | "JSON malformado na requisição" |
| `MissingServletRequestParameterException` | 400 | "Parâmetro 'x' está faltando" |
| `MethodArgumentTypeMismatchException` | 400 | parâmetro não convertido para o tipo |
| `IllegalArgumentException` | 400 | `ex.getMessage()` |
| `BoilerplateException` | 400 | `ex.getMessage()` |
| `AuthenticationException` | 401 | "Não autenticado" |
| `AccessDeniedException` | 403 | "Acesso negado" |
| `EntityNotFoundException`, `NoHandlerFoundException` | 404 | — |
| `HttpRequestMethodNotSupportedException` | 405 | — |
| `DuplicateEntityException`, `EntityExistsException`, `ObjectOptimisticLockingFailureException`, `DataIntegrityViolationException` (constraint) | 409 | — |
| `HttpMediaTypeNotSupportedException` | 415 | — |
| `BusinessValidationException` | 422 | `ex.getMessage()` |
| `HttpMessageNotWritableException`, `Exception` (qualquer outra) | 500 | "Ocorreu um erro interno no servidor" (log `error`) |

Exemplo de resposta de validação:

```json
{
  "status": "BAD_REQUEST",
  "timestamp": "03-10-2026 10:15:00",
  "message": "Erro de validação",
  "subErrors": [
    { "object": "produtoCreateDTO", "field": "nome", "rejectedValue": "", "message": "O nome e obrigatorio" }
  ]
}
```

Para tratar uma exceção nova, adicione um `@ExceptionHandler` no `RestExceptionHandler` e use o
`buildResponseEntity(ApiError)`. Não crie um segundo `@ControllerAdvice`. Filtros servlet nunca chegam ao
handler: para responder erro de dentro de um filtro use `ApiErrorWriter.write(request, response, status, mensagem)`
(`error/ApiErrorWriter.java`), nunca `sendError`. O campo `path` do
`ApiError` é preenchido pelo próprio `buildResponseEntity` com o `getRequestURI()` da requisição em curso
(sem query string); por isso todo handler novo o recebe sem código extra. Coberto por
`RestExceptionHandlerTest`.

---

## Multi-tenancy: filtros de tenant

**Pacote**: `rest.infrastructure.filter`. Ambos são `@Component` implementando
`jakarta.servlet.Filter`.

**`TenantContextFilter`** (`@Order(1)`): lê o tenant do header `X-TENANT-ID` (ou `X-Tenant-Id`) e
chama `ArchbaseTenantContext.setTenantId(...)` (`br.com.archbase.ddd.context`). Se o header não vier,
usa `archbase.app.tenant.default.id` (`@Value("${archbase.app.tenant.default.id:}")`). No `finally`
faz `ArchbaseTenantContext.clear()`.

**`TenantAccessValidationFilter`** (`@Order(2)`): para requisições autenticadas, fora de
`/actuator`, `/swagger-ui`, `/v3/api-docs` e `/api/v1/public`, compara o tenant do contexto com o
`getTenantId()` do principal (via reflection). Responde **403** com um `ApiError` em JSON (via `ApiErrorWriter`) quando forem diferentes e também quando o tenant do
usuário não puder ser determinado (principal ausente, sem `getTenantId`, tenant em branco ou erro na
reflection): o filtro **falha fechado**. Só não valida quando não há tenant no contexto ou a requisição
não está autenticada (a autorização fica a cargo da cadeia do Spring Security). Coberto por
`TenantAccessValidationFilterTest`.

Chaves relacionadas em `application.yml`:

```yaml
archbase:
  multitenancy:
    enabled: true
  app:
    tenant:
      default:
        id: ${ARCHBASE_DEFAULT_TENANT_ID:a9f814d2-4dae-41f3-851b-8aa3d4706561}
      accept-query-param: false      # tenant por ?query vaza em log/Referer
      fail-on-missing: false         # há tenant padrão; num produto multi-tenant real, remova o
                                     # padrão e ligue esta flag (senão dado cai no tenant padrão)
```

Entidades multi-tenant estendem `TenantPersistenceEntityBase` (ver `04-entidade.md`); a coluna
`tenant_id` é `NOT NULL`.

---

## Seeds

**Pacote**: `rest.seed`. Ambos são `CommandLineRunner`.

**`AdminSeedLoader`** (`@Order(50)`, sem `@Profile`): cria o primeiro administrador **só se
`userRepository.count() == 0`** (`UserJpaRepository` do `archbase-security`). Resolve o impasse
"todas as rotas exigem login e não há usuário".

| Chave | Padrão | Efeito |
|-------|--------|--------|
| `archbase.boilerplate.seed.admin.enabled` | `true` | `false` não cria nada |
| `archbase.boilerplate.seed.admin.email` | `admin@archbase.com.br` | e-mail e `userName` |
| `archbase.boilerplate.seed.admin.password` | vazio | senha explícita |

Senha quando vazia: perfil ativo contendo `dev` ou `h2` -> `admin`; qualquer outro -> aleatória
(18 bytes, Base64 URL), escrita **uma vez** no log em `WARN`. O usuário nasce com
`isAdministrator(true)` e o tenant `archbase.app.tenant.default.id`.

**`DataSeedLoader`** (`@Order(100)`, `@Profile("dev")`): cria 16 produtos de exemplo chamando
`ProdutoService.criar(ProdutoCreateDTO)`; falhas por SKU duplicado viram `warn` e a carga segue.
Só roda no perfil `dev`.

---

## SegredoJwtValidator

`rest.infrastructure.config.SegredoJwtValidator` (`@Component`, `@PostConstruct`) lê
`archbase.security.jwt.secret-key` e **impede a aplicação de subir** se o segredo:
- estiver vazio;
- for o placeholder `change-this-secret-key-in-production`;
- não for Base64 válido;
- decodificar para menos de 32 bytes (HS256 exige 256 bits).

Motivo: com segredo inválido a autenticação valida a senha e só a emissão do token quebra, em HTTP
500 sem pista. Gerar um segredo:

```bash
echo "ARCHBASE_JWT_SECRET=$(openssl rand -base64 48)" >> .env
```

Consequência prática: o `application.yml` traz o placeholder como default, então **sem
`ARCHBASE_JWT_SECRET` no ambiente a aplicação não sobe** (de propósito).

---

## RateLimitingConfig e RateLimitingFilter

`rest.infrastructure.config.RateLimitingConfig` (`@Configuration`, Bucket4j 7.6) cria os buckets e os
guarda em caches Caffeine (expiração e teto de 100 mil entradas, para não crescer sem limite).
`rest.infrastructure.filter.RateLimitingFilter` (`@Order(3)`, depois do Spring Security) aplica:

| Perfil | Chave | Padrão |
|--------|-------|--------|
| `/api/v1/webhooks/**` | IP (`getRemoteAddr()`) | 100/min |
| autenticado | nome do usuário | 1000/min |
| anônimo | IP (`getRemoteAddr()`) | 50/min |

Excedido o limite, responde **429** com `Retry-After`. `/actuator`, `/swagger-ui` e `/v3/api-docs` não são
limitados. Só `getRemoteAddr()` é usado, nunca `X-Forwarded-For`; atrás de proxy, configure
`server.forward-headers-strategy`. O estado é local à instância (com N réplicas o limite efetivo é N vezes).

```yaml
security:
  rate-limit:
    enabled: true                                   # false desliga o filtro
    webhook:       { limit: 100,  duration: 60 }
    authenticated: { limit: 1000, duration: 60 }
    ip:            { limit: 50,   duration: 60 }    # duration em segundos
```

O 429 também sai como `ApiError`. Coberto por `RateLimitingFilterTest`.

---

## BoilerplateRoleResolver

`rest.infrastructure.config.BoilerplateRoleResolver` implementa `ArchbaseRoleResolver`
(`br.com.archbase.security.spi`) devolvendo `Set.of()` e `isOwner = false`. Com
`require-role.no-resolver-policy: deny`, `@RequireRole` nega tudo até ser implementado. Detalhes e
ressalvas em `12-seguranca.md`.

Outros beans de configuração no mesmo pacote: `BoilerplateEmailService` (implementa
`ArchbaseEmailService`; **não envia e-mail**, só registra em log — troque pelo seu provedor, senão o
reset de senha não entrega nada), `JacksonConfig`, `JedisConfig`, `QueryDslConfig`
(`JPAQueryFactory`), `RestTemplateConfig`, `WebClientConfig`.

---

## OpenAPIConfig

`rest.infrastructure.config.OpenAPIConfig`: define o bean `OpenAPI` (título "Archbase Boilerplate
API", `v1.0.0`), o esquema de segurança HTTP bearer JWT chamado **`bearerAuth`** (é o nome usado em
`@SecurityRequirement(name = "bearerAuth")` nos controllers) e dois `GroupedOpenApi`: `public`
(`/api/**`) e `admin` (`/admin/**`). Servidores:

```yaml
openapi:
  server:
    dev:     { url: http://localhost:${APP_PORT:8080}, description: Development server }
    homolog: { url: ${OPENAPI_HOMOLOG_URL:}, description: Homologation server }
    prod:    { url: ${OPENAPI_PROD_URL:},    description: Production server }
```

Homolog e prod só entram na lista se a URL não estiver vazia. Swagger UI: `/swagger-ui/**`
(na whitelist de segurança).

---

## Perfis e application*.yml

`application.yml` define `spring.profiles.active: ${APP_PROFILE:dev}` — **o perfil padrão é `dev`**.

| Arquivo | Perfil | Pontos principais |
|---------|--------|-------------------|
| `application.yml` | sempre | datasource PostgreSQL (`POSTGRES_HOST/PORT/DATABASE/USER/PASSWORD`), Hikari, `ddl-auto: update`, `open-in-view: true`, cache Ehcache, Redis, `spring.data.rest.detection-strategy: annotated`, `server.port: ${APP_PORT:8080}`, bloco `archbase.*`, `openapi.*`, `security.rate-limit.*` |
| `application-dev.yml` | `dev` | Flyway ligado (`APP_FLYWAY_ENABLED`, `baseline-on-migrate`), `ddl-auto: update`, `show_sql`, logs `DEBUG`, actuator com tudo exposto; ativa o `DataSeedLoader` |
| `application-h2.yml` | `h2` | H2 em memória (`jdbc:h2:mem:archbasedb`), console `/h2-console`, `ddl-auto: update`. Escolha consciente: `mvn spring-boot:run -Dspring-boot.run.profiles=h2` |
| `application-homolog.yml` | `homolog` | `ddl-auto: update`, logs `INFO` |
| `application-prod.yml` | `prod` | Flyway ligado, **`ddl-auto: validate`**, logs `WARN`/`INFO` com arquivo `logs/archbase-boilerplate.log`, actuator só `health,info,metrics`, `show-details: never` |

Observações: o perfil `h2` precisa ser ativado sozinho de propósito (o `AdminSeedLoader` trata `h2`
como desenvolvimento); o bloco `datasource.postgres.*` do `application-prod.yml` usa chaves que o
`application.yml` base não lê (a conexão efetiva vem de `spring.datasource.*` com as variáveis
`POSTGRES_*`).

### Chaves `archbase.*` do application.yml

| Chave | Valor no boilerplate |
|-------|---------------------|
| `archbase.multitenancy.enabled` | `true` |
| `archbase.app.tenant.default.id` | `${ARCHBASE_DEFAULT_TENANT_ID:a9f814d2-...}` |
| `archbase.app.tenant.accept-query-param` / `fail-on-missing` | `false` / `false` |
| `archbase.jpa.repositories` / `archbase.jpa.entities` | pacotes `...core.infrastructure.output.persistence.repository` / `.entity` |
| `archbase.component.scan` | `br.com.archbase.boilerplate` |
| `archbase.security.*` | ver `12-seguranca.md` (JWT, whitelist, CORS, senha, hardening, ...) |
| `archbase.boilerplate.seed.admin.*` | lidas pelo `AdminSeedLoader` (padrões no código, não no yml) |

---

## Migrations Flyway

**Pasta**: `archbase-boilerplate-rest/src/main/resources/db/migration`. Hoje só existe
`V1__schema_inicial.sql`, que contém `produto` e as tabelas `seguranca*` do Archbase
(`seguranca`, `seguranca_acao`, `seguranca_evento`, `seguranca_grupo_usuario`,
`seguranca_horario_acesso`, `seguranca_intervalo_acesso`, `seguranca_permissao`,
`seguranca_recurso`, `seguranca_revisao`, `seguranca_token_acesso`, `seguranca_token_api`,
`seguranca_token_redefinicao_senha`).

Regras:
- O arquivo é **gerado** (`pg_dump --schema-only` de um banco criado com `ddl-auto=create`), não
  escrito à mão. Para regerar: PostgreSQL vazio, rode a aplicação uma vez com `ddl-auto=create` e
  refaça o dump.
- Em `prod`, `ddl-auto: validate` exige que **todas** as tabelas existam; sem migration para uma
  entidade nova a aplicação não sobe. Nova entidade = nova `V{n}__descricao.sql`.
- As tabelas `_AUD` não estão na V1: só são necessárias com `archbase.security.audit.enabled=true`.
- `flyway-core`, `flyway-database-postgresql` e `archbase-starter-flyway` estão no pom do `core`.
- Em `dev` o Flyway também roda (para a migration quebrada aparecer cedo), mas o `ddl-auto: update`
  continua ativo por conveniência; troque para `validate` localmente ao menos uma vez ao criar uma
  entidade.

---

## Testes

**`ProdutoServiceTest`** (`core/src/test/.../application/service`): unitário puro com
`@ExtendWith(MockitoExtension.class)`, `@MockitoSettings(strictness = LENIENT)`, `@Mock
ProdutoJpaRepository` e `@InjectMocks ProdutoService`. Organizado em `@Nested` com `@DisplayName` em
português (criar, atualizar, buscarPorId, remover, status, busca); asserções com AssertJ
(`assertThat`, `assertThatThrownBy`). Casos como "SKU já existe" esperam
`DuplicateEntityException` e "não encontrado" esperam `EntityNotFoundException`.

**`AplicacaoSobeTest`** (`rest/src/test/.../rest`): `@SpringBootTest(classes =
ArchbaseBoilerplateApplication.class, webEnvironment = RANDOM_PORT)` contra **PostgreSQL real**.
Em `@BeforeAll` usa `assumeTrue(bancoDisponivel())`: sem banco o teste é **pulado**, não falha.
Configuração por `-Dboilerplate.test.postgres.{host,port,db,user,password}` ou variáveis
`POSTGRES_*`; `@DynamicPropertySource` injeta datasource e um `archbase.security.jwt.secret-key`
Base64 de teste (o placeholder do yml derrubaria o `SegredoJwtValidator`). Casos:

- o contexto sobe com Flyway e banco real;
- criar preenche `id` e `createEntityDate` e o produto é recuperável;
- atualizar preenche `updateEntityDate`;
- `ArchbasePasswordStrengthPolicy` está ligada e recusa senha fraca (`ArchbaseValidationException`);
- recuperação de senha (`POST /api/v1/auth/sendResetPasswordEmail/{email}`) responde igual para e-mail
  existente e inexistente (via `java.net.http.HttpClient`, não `TestRestTemplate`, que saiu do
  Spring Boot 4).

Rodar:

```bash
docker compose up -d postgres
mvn test -Dtest=AplicacaoSobeTest
make test        # roda o mvn test do projeto todo
```
