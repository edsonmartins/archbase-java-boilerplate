# 12. Segurança Archbase

Anotações de segurança do Archbase Framework.

---

## Conceito

**CRÍTICO**: Use SEMPRE anotações Archbase, NUNCA Spring Security:
- `@HasPermission` - **a principal**: exige uma ação sobre um recurso
- `@RequireRole` - Roles do sistema (leia a ressalva abaixo antes de usar)
- `@RequireProfile` - Perfis de acesso
- `@RequirePersona` - Personas em contexto

**PROIBIDO**: `@PreAuthorize`, `@Secured`, `@RolesAllowed`

---

## ⚠️ Leia antes de usar `@RequireRole`

**`@RequireRole` não protege nada sozinho.** As roles que ele confere pertencem ao domínio da
aplicação, não ao Archbase — o framework não sabe o que é "ADMIN" no seu sistema. Para a anotação
decidir alguma coisa, a aplicação precisa registrar um bean `ArchbaseRoleResolver`.

**Sem esse bean, quem decide é uma chave cujo padrão é liberar:**

```yaml
archbase:
  security:
    require-role:
      no-resolver-policy: permit   # padrão: passa. Use 'deny' para negar.
```

Ou seja: anotar um endpoint com `@RequireRole("ADMIN")` num projeto sem resolver o deixa **aberto a
qualquer autenticado**, sem erro nem aviso. O código parece protegido e não está.

Duas saídas, nesta ordem de preferência:

1. **Use `@HasPermission`** (abaixo). Ele funciona com o modelo de permissões do próprio Archbase e
   não depende de bean nenhum da aplicação.
2. Se precisar mesmo de roles, **registre o `ArchbaseRoleResolver`** e considere
   `no-resolver-policy: deny`, para que a ausência do bean falhe alto em vez de liberar em silêncio.

---

## @HasPermission — o caminho padrão

Exige uma **ação** sobre um **recurso**, que é como o Archbase modela permissão. Não depende de nada
que a aplicação precise implementar.

```java
import br.com.archbase.security.annotation.HasPermission;   // 'annotation', no singular

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    @GetMapping
    @HasPermission(action = "VIEW", resource = "PEDIDO", description = "Listar pedidos")
    public ResponseEntity<List<PedidoDto>> listar() { ... }

    @PostMapping
    @HasPermission(action = "CREATE", resource = "PEDIDO", description = "Criar pedido")
    public ResponseEntity<PedidoDto> criar(@RequestBody PedidoDto dto) { ... }
}
```

**Só em método.** `@HasPermission` é `@Target(METHOD)` — não compila na classe. As outras três
aceitam classe, e nesse caso valem para todos os métodos, com a anotação de método sobrescrevendo a
da classe.

Repare no pacote: `@HasPermission` está em `...security.annotation` (singular) e as outras três em
`...security.annotations` (plural). É fácil errar o import e não entender por que não compila.

---

## @RequireRole

```java
package br.com.empresa.projeto.rest.infrastructure.input.rest;

import br.com.archbase.security.annotations.RequireRole;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/examples")
public class AdminExampleController {

    @GetMapping
    @RequireRole({"ADMIN", "SUPERVISOR"})
    public ResponseEntity<List<ExampleDTO>> findAll() {
        // Apenas ADMIN e SUPERVISOR podem acessar
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public void delete(@PathVariable String id) {
        // Apenas ADMIN pode deletar
    }

    @PostMapping
    @RequireRole(value = {"ADMIN", "GERENTE", "SUPERVISOR"})
    public ResponseEntity<ExampleDTO> create(@RequestBody CreateExampleDTO dto) {
        // ADMIN, GERENTE ou SUPERVISOR podem criar
    }
}
```

---

## @RequireProfile

```java
@RestController
@RequestMapping("/api/v1/examples")
public class ExampleController {

    @PostMapping
    @RequireProfile("MANAGER")
    public ResponseEntity<ExampleDTO> create(@RequestBody CreateExampleDTO dto) {
        // Apenas users com profile MANAGER
    }

    @GetMapping("/relatorio")
    @RequireProfile({"MANAGER", "ANALYST"})
    public ResponseEntity<RelatorioDTO> gerarRelatorio() {
        // MANAGER ou ANALYST
    }
}
```

---

## @RequirePersona

```java
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @DeleteMapping("/{id}")
    @RequirePersona(value = "ADMIN", context = "SYSTEM")
    public void delete(@PathVariable String id) {
        // Apenas ADMIN no contexto SYSTEM
    }

    @PostMapping("/reset")
    @RequirePersona(value = "SUPERVISOR", context = "TENANT")
    public void reset() {
        // SUPERVISOR no contexto TENANT
    }
}
```

---

## Combinação de Anotações

```java
@RestController
@RequestMapping("/api/v1/examples")
public class ExampleController {

    @PostMapping
    @RequireRole({"ADMIN", "GERENTE"})
    @RequireProfile("MANAGER")
    public ResponseEntity<ExampleDTO> create(@RequestBody CreateExampleDTO dto) {
        // Precisa ter role E profile
    }

    @GetMapping("/sensiveis")
    @RequireRole("ADMIN")
    @RequirePersona(value = "ADMIN", context = "SYSTEM")
    public ResponseEntity<List<DadoSensivelDTO>> getDadosSensiveis() {
        // Múltiplas condições (todas devem ser satisfeitas)
    }
}
```

---

## Nível de Método vs Classe

```java
@RestController
@RequestMapping("/api/v1/admin")
@RequireRole("ADMIN")  // Todos os métodos requerem ADMIN
public class AdminController {

    @GetMapping
    public ResponseEntity<List<ExampleDTO>> findAll() {
        // Herda @RequireRole("ADMIN") da classe
    }

    @GetMapping("/publico")
    @RequireRole  // Sobrescreve - endpoint público
    public ResponseEntity<String> publicEndpoint() {
        // Qualquer um pode acessar
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})  // Sobrescreve com roles específicos
    public void delete(@PathVariable String id) {
        // ADMIN ou SUPER_ADMIN
    }
}
```

---

## Configuração application.yml

```yaml
archbase:
  security:
    jwt:
      secret-key: ${JWT_SECRET:change-this-secret-key}
      token-expiration: 86400000
      refresh-expiration: 604800000
    scan-packages: br.com.empresa.projeto.rest.infrastructure.input.rest
    whitelist: /actuator/health,/swagger-ui/**,/v3/api-docs/**,/api/v1/public/**
    cors:
      allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
      allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
      allowed-headers: Authorization,Content-Type,Accept
```

**CORS tem uma fonte só.** É esta. Não crie um `CorsFilter` na aplicação: ele competiria com o do
framework, e duas regras para a mesma coisa divergem no primeiro dia em que alguém edita só uma.

### Chaves que costumam ser esquecidas

```yaml
archbase:
  security:
    # Confere o esquema de segurança na subida e cria o que falta (tabelas e colunas que uma versão
    # nova do framework passou a exigir). Só comandos aditivos; nunca remove nada.
    schema:
      mode: apply          # apply | report | off
    # Tela de diagnóstico de acesso: árvore de quem tem o quê, panorama e simulação.
    # SEM esta chave o controller nem é registrado e a tela responde 404 — não 403.
    diagnostics:
      enabled: true
    # Trilha de auditoria: quem alterou permissão, quem entrou, quem teve acesso negado.
    # Ligue DEPOIS de garantir que as tabelas existem (ver abaixo).
    audit:
      enabled: false
```

### Se o projeto sobe com `hibernate.ddl-auto: validate`

Duas tabelas precisam existir **antes** do boot, mesmo com a trilha desligada: `seguranca_evento` e
`seguranca_revisao`. São entidades JPA comuns, então o Hibernate as exige na validação — e a rotina
de `schema.mode` não ajuda aqui, porque roda **depois** que o `EntityManagerFactory` sobe.

Sintoma: `Schema validation: missing table [seguranca_evento]`, e a aplicação não sobe.

O DDL está em `deployment/sql/` no repositório do framework.

---

## Obter Usuário Autenticado

```java
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    @GetMapping
    public ResponseEntity<UserDTO> getMe() {
        // Usar Archbase Security Context
        String userId = ArchbaseSecurityContext.getCurrentUserId();
        String tenantId = ArchbaseSecurityContext.getCurrentTenantId();

        return ResponseEntity.ok(userService.findById(userId));
    }
}
```

---

## Verificar Roles Programaticamente

```java
@Service
@RequiredArgsConstructor
public class ExampleService {

    private final ArchbaseSecurityService securityService;

    public void realizarAcao() {
        if (securityService.hasRole("ADMIN")) {
            // Lógica específica para ADMIN
        }

        if (securityService.hasProfile("MANAGER")) {
            // Lógica para MANAGER
        }

        String currentRole = securityService.getCurrentRole();
        String currentProfile = securityService.getCurrentProfile();
    }
}
```

---

## Roles Comuns

| Role | Descrição |
|------|-----------|
| `ADMIN` | Administrador do sistema |
| `SUPERVISOR` | Supervisor de área |
| `GERENTE` | Gerente |
| `USUARIO` | Usuário padrão |
| `CONVIDADO` | Acesso limitado |

---

## Profiles Comuns

| Profile | Descrição |
|---------|-----------|
| `ADMIN` | Administrativo |
| `MANAGER` | Gerencial |
| `USER` | Usuário final |
| `ANALYST` | Analista |
| `OPERATOR` | Operador |

---

## Tabela Comparativa

| ERRADO | CORRETO |
|--------|---------|
| `@PreAuthorize("hasRole('ADMIN')")` | `@RequireRole("ADMIN")` |
| `@Secured("ROLE_ADMIN")` | `@RequireRole("ADMIN")` |
| `@RolesAllowed("ADMIN")` | `@RequireRole("ADMIN")` |
| `if (request.isUserInRole("ADMIN"))` | `securityService.hasRole("ADMIN")` |

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **@RequireRole** | Para autorização por role |
| **@RequireProfile** | Para perfis de acesso |
| **@RequirePersona** | Para personas em contexto específico |
| **Whitelist** | Configurar endpoints públicos no yml |
| **NUNCA @PreAuthorize** | Use anotações Archbase |

---

**CRÍTICO**: Apenas anotações Archbase funcionam corretamente com a infraestrutura de multi-tenancy.
