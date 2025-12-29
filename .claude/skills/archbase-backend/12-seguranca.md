# 12. Segurança Archbase

Anotações de segurança do Archbase Framework.

---

## Conceito

**CRÍTICO**: Use SEMPRE anotações Archbase, NUNCA Spring Security:
- `@RequireRole` - Roles do sistema
- `@RequireProfile` - Perfis de acesso
- `@RequirePersona` - Personas em contexto

**PROIBIDO**: `@PreAuthorize`, `@Secured`, `@RolesAllowed`

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
