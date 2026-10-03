# 12. Segurança Archbase

Anotações e configuração de segurança do Archbase Framework (`archbase-security`).

---

## Como o boilerplate protege hoje

Fatos do código atual, que este arquivo toma como ponto de partida:

- O `ProdutoController` **não tem nenhuma anotação de autorização**. Ele só tem
  `@SecurityRequirement(name = "bearerAuth")`, que é documentação OpenAPI e não autoriza nada.
- A proteção real é: **tudo exige autenticação (JWT), exceto o que está em
  `archbase.security.whitelist`** (`/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`,
  `/api/v1/public/**`).
- Não há uso de `@PreAuthorize`, `@Secured` ou `@RolesAllowed` em nenhum lugar do código. O que
  existe de Spring Security direto é o `AccessDeniedException`/`AuthenticationException` tratados no
  `RestExceptionHandler`, o `SecurityContextHolder` lido no `TenantAccessValidationFilter` e o
  `PasswordEncoder` usado pelo `AdminSeedLoader`.
- `@RequireRole` **não é usado**, mas o boilerplate registra um `BoilerplateRoleResolver` e configura
  `no-resolver-policy: deny` (ver abaixo).

**Convenção recomendada para código novo:** prefira as anotações do Archbase (`@HasPermission` à
frente), porque elas conversam com o modelo de permissões, o diagnóstico de acesso e a auditoria do
framework. O boilerplate não proíbe `@PreAuthorize`; só não o usa, e nada no código ou no yml o
bloqueia. Se misturar, lembre que a decisão do Archbase e a do Spring são independentes: um endpoint
anotado só com `@PreAuthorize` não aparece no catálogo de permissões do Archbase.

As anotações do Archbase:
- `@HasPermission` - **a principal**: exige uma ação sobre um recurso
- `@RequireRole` - roles de negócio da aplicação (leia a ressalva abaixo antes de usar)
- `@RequireProfile` - perfis de acesso
- `@RequirePersona` - personas em contexto

---

## ⚠️ Leia antes de usar `@RequireRole`

**`@RequireRole` não protege nada sozinho.** As roles que ele confere pertencem ao domínio da
aplicação, não ao Archbase — o framework não sabe o que é "ADMIN" no seu sistema. Para a anotação
decidir alguma coisa, a aplicação precisa registrar um bean `ArchbaseRoleResolver`.

**Sem esse bean, quem decide é uma chave cujo padrão do framework é liberar:**

```yaml
archbase:
  security:
    require-role:
      no-resolver-policy: permit   # padrão do framework: passa. Use 'deny' para negar.
```

Ou seja: anotar um endpoint com `@RequireRole("ADMIN")` num projeto sem resolver, e com a chave no
padrão, o deixa **aberto a qualquer autenticado**, sem erro nem aviso. O código parece protegido e
não está.

**O que o boilerplate faz a respeito** (`application.yml` + `BoilerplateRoleResolver`):

1. `archbase.security.require-role.no-resolver-policy: deny` — já vem endurecido.
2. Registra `BoilerplateRoleResolver implements ArchbaseRoleResolver`, que devolve `Set.of()` em
   `resolveRoles` e `false` em `isOwner`. Resultado: qualquer método com `@RequireRole` **nega** até
   alguém implementar a consulta real de roles. É o oposto de liberar por omissão.
3. Com `deny`, **apagar** o `BoilerplateRoleResolver` faz a aplicação recusar subir (o framework não
   mantém uma proteção que não teria como funcionar). Para removê-lo, volte a chave para `permit`.

Para usar `@RequireRole` de verdade, troque o corpo do resolver:

```java
package br.com.archbase.boilerplate.rest.infrastructure.config;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.spi.ArchbaseRoleResolver;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BoilerplateRoleResolver implements ArchbaseRoleResolver {

    @Override
    public Set<String> resolveRoles(UserEntity user) {
        return lojaRepository.findRolesDoUsuario(user.getId());   // consulta ao SEU modelo
    }

    @Override
    public boolean isOwner(UserEntity user) {
        return false;   // implemente ao usar @RequireRole(ownerOnly = true)
    }
}
```

Duas saídas, nesta ordem de preferência:

1. **Use `@HasPermission`** (abaixo). Funciona com o modelo de permissões do próprio Archbase e não
   depende de bean da aplicação.
2. Se precisar de roles, **implemente o `ArchbaseRoleResolver`** e mantenha `no-resolver-policy: deny`.

---

## @HasPermission — o caminho padrão

Exige uma **ação** sobre um **recurso**, que é como o Archbase modela permissão.

```java
import br.com.archbase.security.annotation.HasPermission;   // 'annotation', no singular

@RestController
@RequestMapping("/api/v1/produtos")
public class ProdutoController {

    @GetMapping("/ativos")
    @HasPermission(action = "VIEW", resource = "PRODUTO", description = "Listar produtos ativos")
    public ResponseEntity<List<ProdutoDTO>> buscarAtivos() { ... }

    @PostMapping
    @HasPermission(action = "CREATE", resource = "PRODUTO", description = "Criar produto")
    public ResponseEntity<ProdutoDTO> criar(@Valid @RequestBody ProdutoCreateDTO dto) { ... }
}
```

(Exemplo ilustrativo: o `ProdutoController` real **não** tem `@HasPermission`.)

Atributos reais (verificados em `archbase-security-3.2.2`): `action` e `description` são obrigatórios;
`resource`, `minimumLevel` (`AccessLevel`, padrão `NONE`), `tenantId`, `companyId` e `projectId` são
opcionais.

**Só em método.** `@HasPermission` é `@Target(METHOD)` — não compila na classe. `@RequireRole`,
`@RequireProfile` e `@RequirePersona` aceitam `METHOD` e `TYPE`.

**Pacotes diferentes** — é fácil errar o import:

| Anotação | Pacote |
|----------|--------|
| `@HasPermission`, `@ArchbaseResource`, `@ArchbaseSecurityAdminEndpoint` | `br.com.archbase.security.annotation` (singular) |
| `@RequireRole`, `@RequireProfile`, `@RequirePersona` | `br.com.archbase.security.annotations` (plural) |
| `ArchbaseRoleResolver` | `br.com.archbase.security.spi` |

---

## @RequireRole

`value` é **obrigatório** (`String[]`). Outros atributos: `requireAll` (padrão `false`, ou seja, basta
uma das roles), `requirePlatformAdmin`, `ownerOnly`, `context`, `allowSystemAdmin` (padrão `true`) e
`message`. Só funciona com `ArchbaseRoleResolver` implementado (ver ressalva).

```java
import br.com.archbase.security.annotations.RequireRole;

@GetMapping
@RequireRole({"ADMIN", "SUPERVISOR"})          // uma das duas basta
public ResponseEntity<List<ExampleDTO>> findAll() { ... }

@DeleteMapping("/{id}")
@RequireRole(value = {"ADMIN", "GERENTE"}, requireAll = true)   // as duas
public void delete(@PathVariable String id) { ... }
```

---

## @RequireProfile

`value` obrigatório. Outros: `requireAll`, `resource`, `action`, `allowSystemAdmin`,
`requireActiveUser` (padrão `true`), `message`.

```java
import br.com.archbase.security.annotations.RequireProfile;

@PostMapping
@RequireProfile("MANAGER")
public ResponseEntity<ExampleDTO> create(@RequestBody CreateExampleDTO dto) { ... }

@GetMapping("/relatorio")
@RequireProfile({"MANAGER", "ANALYST"})        // MANAGER ou ANALYST
public ResponseEntity<RelatorioDTO> gerarRelatorio() { ... }
```

---

## @RequirePersona

`value` obrigatório. Outros: `requireAll`, `context`, `contextData`, `allowSystemAdmin`,
`requireActiveUser`, `ownerOnly`, `resource`, `action`, `message`.

```java
import br.com.archbase.security.annotations.RequirePersona;

@DeleteMapping("/{id}")
@RequirePersona(value = "ADMIN", context = "SYSTEM")
public void delete(@PathVariable String id) { ... }
```

---

## Combinação e nível de classe

Anotações de classe (`@RequireRole`, `@RequireProfile`, `@RequirePersona`) valem para todos os métodos
do controller; a de método a sobrescreve. Combinar mais de uma no mesmo método exige que todas sejam
satisfeitas.

```java
@RestController
@RequestMapping("/api/v1/admin")
@RequireRole("ADMIN")                      // todos os métodos
public class AdminController {

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"}) // sobrescreve a da classe
    public void delete(@PathVariable String id) { ... }
}
```

Não existe forma de "esvaziar" `@RequireRole` para tornar um método público (`value` é obrigatório).
Endpoint público se declara na `archbase.security.whitelist`, não por anotação.

---

## Configuração real: `archbase.security.*` (application.yml)

O `application.yml` do boilerplate já nasce endurecido. Resumo do que está lá:

```yaml
archbase:
  security:
    jwt:
      secret-key: ${ARCHBASE_JWT_SECRET:change-this-secret-key-in-production}  # validado, ver 14
      token-expiration: 86400000
      refresh-expiration: 604800000
      strict-token-use: true            # recusa token sem o claim token_use
      accept-token-query-param: false   # nada de ?token=
    scan-packages: br.com.archbase.boilerplate.rest.infrastructure.input.rest
    whitelist: /actuator/health,/swagger-ui/**,/v3/api-docs/**,/api/v1/public/**
    prevent-user-enumeration: true      # recuperação de senha responde igual p/ qualquer e-mail
    password-change:
      revoke-sessions: true
    require-role:
      no-resolver-policy: deny
    public-paths:
      actuator: false
      registration: false               # sem auto-cadastro anônimo
      legacy-app-routes: false
    admin-guard:
      enabled: true
      allow-unverifiable-principal: false
    admin-endpoints:
      policy: permit                    # de propósito, por causa do archbase-react (ver comentário no yml)
    api-token:
      hash-enabled: true
      purge-plaintext: false            # irreversível; ligar só depois
    client-ip:
      trust-forwarded-for: false        # só ligue com proxy que sobrescreva X-Forwarded-For
    password:
      min-length: 12
      require-digit: true
      require-uppercase: true
      require-lowercase: true
      require-special: true
      block-common: true
    hardening:
      validation: fail                  # recusa subir se alguma proteção ligada não puder funcionar
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:4200}
      allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
      allowed-headers: Authorization,Content-Type,Accept,X-Requested-With,X-Device-ID,X-App-Version,X-TENANT-ID,X-Tenant-Id
      allow-credentials: true
```

Cuidados:
- **CORS tem uma fonte só**: `archbase.security.cors`. Não crie um `CorsFilter` na aplicação — ele
  competiria com o do framework. (O boilerplate já removeu o bloco `application.security.*`, que
  ninguém lia.)
- **`admin-endpoints.policy: permit`** significa que qualquer autenticado alcança endpoints
  administrativos do framework, incluindo `POST /api/v1/user`. O valor seguro é `admin-only`, mas ele
  derruba o auto-registro de recursos do `archbase-react` (`POST /api/v1/resource/register`) para
  não-administradores. Se o projeto não usa archbase-react, troque para `admin-only`.
- **`strict-token-use: true`** só é seguro num projeto novo; em projeto com tokens já emitidos, ligar
  depois que o maior `refresh-expiration` passar.
- A política de senha (`password.*`) tem defaults do framework todos desligados; o teste
  `AplicacaoSobeTest` falha se as chaves forem apagadas (ver `14-infraestrutura.md`).
- O segredo do JWT é conferido na subida pelo `SegredoJwtValidator` (ver `14-infraestrutura.md`).

### Chaves do framework que o boilerplate NÃO define (existem no `archbase-security`)

```yaml
archbase:
  security:
    schema:
      mode: apply          # apply | report | off — confere/cria o que falta do esquema de segurança
    diagnostics:
      enabled: true        # tela de diagnóstico de acesso; sem a chave o controller nem é registrado (404)
    audit:
      enabled: false       # trilha de auditoria; ligue só depois de garantir as tabelas
```

Se `audit.enabled` for ligado, as tabelas `_AUD` são necessárias: não estão na `V1__schema_inicial.sql`
(o DDL está em `deployment/sql/` no repositório do framework). Já `seguranca_evento` e
`seguranca_revisao` **estão** na V1, porque com `ddl-auto: validate` (perfil `prod`) o Hibernate as
exige mesmo com a trilha desligada.

---

## Usuário autenticado e verificação programática

O boilerplate tem um `SecurityService` próprio (`@Service("securityService")`, em
`br.com.archbase.boilerplate.core.application.service.security`). Ele **não** é do Archbase: é código
do projeto, para uso programático. Métodos existentes: `hasRole`, `hasProfile`, `isAdmin`,
`isOwner`, `isOwnerOrAdmin`, `anyOf`, `allOf`, `getCurrentUser`, `getCurrentUserId`,
`getCurrentUsername`, `getCurrentUserName`, `getCurrentTenantId`, `getCurrentRole`,
`getCurrentProfile`, `canAccessTenant`, `canAccessTenantOrAdmin`, `isAuthenticated`.

```java
import br.com.archbase.boilerplate.core.application.service.security.SecurityService;

@Service
@RequiredArgsConstructor
public class ExampleService {

    private final SecurityService securityService;

    public void realizarAcao() {
        if (securityService.isAdmin()) { /* lógica de admin */ }
        String userId = securityService.getCurrentUserId();
        String tenantId = securityService.getCurrentTenantId();
    }
}
```

No lado do framework (verificado em `archbase-security` 3.2.2):

- `br.com.archbase.security.service.ArchbaseSecurityService` — `hasPermission(Authentication, action,
  resource, tenantId, companyId, projectId)` e `decide(...)`, que devolve `AccessDecision`.
- `SecurityContextHolder.getContext().getAuthentication()`, como faz o `TenantAccessValidationFilter`.
- Tenant atual: `br.com.archbase.ddd.context.ArchbaseTenantContext.getTenantId()`.

> Versões anteriores desta skill citavam `ArchbaseSecurityContext` e `ArchbaseSecurityService.hasRole`.
> Não existem em `archbase-security` 3.2.2 (a classe `ArchbaseSecurityContext` não foi encontrada nos
> jars). `hasRole`/`hasProfile` existem apenas no `SecurityService` do próprio boilerplate.

---

## Primeiro administrador

Banco novo não tem usuário, e todas as rotas exigem autenticação. O `AdminSeedLoader` resolve isso
(ver `14-infraestrutura.md`): cria `admin@archbase.com.br` quando não existe nenhum usuário; senha
`admin` em `dev`/`h2`, aleatória no log nos demais perfis, ou a de
`archbase.boilerplate.seed.admin.password`.

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **@HasPermission** | Caminho padrão para autorização por ação/recurso |
| **@RequireRole** | Só com `ArchbaseRoleResolver` real e `no-resolver-policy: deny` |
| **Whitelist** | Endpoints públicos declarados em `archbase.security.whitelist` |
| **@SecurityRequirement** | Só documenta no Swagger; não autoriza |
| **CORS** | Uma fonte: `archbase.security.cors` |
| **Testar como não-admin** | Antes de afrouxar qualquer flag de endurecimento |
