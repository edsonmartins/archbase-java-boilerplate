# Archbase Java Boilerplate

> Boilerplate para criação de projetos backend Java com Spring Boot 4 e Archbase Framework 3

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Archbase](https://img.shields.io/badge/Archbase-3.2.1-blue.svg)](https://github.com/edsonmartins/archbase-app-framework)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Características

- **[Spring Boot 4.1.0](https://spring.io/projects/spring-boot)** - Framework web com suporte a Jakarta EE
- **[Java 25](https://openjdk.org/projects/jdk/25/)** - LTS com Virtual Threads (Project Loom)
- **[Archbase Framework 3.2.1](https://github.com/edsonmartins/archbase-app-framework)** - Framework DDD brasileiro completo
- **[QueryDSL 7.2](https://querydsl.com/)** - Queries tipo-safe com JPA (fork openfeign, Jakarta-native)
- **Arquitetura Hexagonal** - Ports e Adapters (limpa e testável)
- **Multi-tenancy** - Suporte nativo a múltiplos tenants
- **Virtual Threads** - Concorrência escalável com Java 25
- **PostgreSQL 16** - Banco de dados principal
- **Flyway** - Migrations de banco de dados versionadas
- **H2** - Para desenvolvimento e testes
- **Docker Compose** - Infraestrutura local completa
- **OpenAPI/Swagger** - Documentação de API integrada
- **Global Exception Handler** - Tratamento centralizado de erros
- **MapStruct** - Mapeamento de objetos eficiente
- **Lombok** - Redução de boilerplate

---

## Stack Tecnológico

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| Java | 25 | LTS com Virtual Threads |
| Spring Boot | 4.1.0 | Framework principal |
| Archbase Framework | 3.2.1 | DDD, Security, Multitenancy |
| QueryDSL | 7.2 | Queries tipo-safe (fork openfeign) |
| PostgreSQL | 16 | Banco de dados |
| Flyway | 10.15.0 | Migrations |
| H2 | 2.2.224 | Desenvolvimento |
| MapStruct | 1.6.3 | Mapeamento |
| Lombok | 1.18.46 | Boilerplate |
| Springdoc OpenAPI | 3.0.0 | Documentação (linha 3 p/ Boot 4) |
| Jetty | - | Servlet container (o starter-tomcat é excluído) |
| JUnit 5 | - | Testes |
| Mockito | - | Mocks para testes |

---

## Estrutura do Projeto

```
archbase-java-boilerplate/
├── archbase-boilerplate-core/           # Domínio, Aplicação e Persistência
│   ├── domain/
│   │   ├── entity/                      # Entidades de domínio
│   │   ├── dto/                         # DTOs (ProdutoDTO, ProdutoCreateDTO, ProdutoUpdateDTO)
│   │   ├── enums/                       # Enumerações
│   │   └── exception/                   # Exceções customizadas
│   ├── application/
│   │   ├── service/                     # Services de negócio
│   │   │   └── security/                # SecurityService
│   │   └── port/out/                    # Portas hexagonais
│   └── infrastructure/output/persistence/
│       ├── entity/                      # Entidades JPA
│       ├── repository/                  # JPA Repositories
│       ├── adapter/                     # QueryDSL Adapters
│       └── mapper/                      # MapStruct Mappers
│
├── archbase-boilerplate-rest/           # REST API
│   └── infrastructure/
│       ├── config/                      # Configurações (CORS, OpenAPI, RateLimit)
│       ├── error/                       # GlobalExceptionHandler, ApiError
│       ├── input/rest/                  # Controllers REST
│       └── seed/                        # DataSeedLoader
│
└── src/main/resources/
    ├── db/migration/                    # Flyway migrations
    ├── application.yml                  # Configuração principal
    └── ehcache.xml                      # Configuração de cache
```

---

## Padrões Implementados

| Padrão | Descrição |
|--------|-----------|
| **Arquitetura Hexagonal** | Separação clara entre Domain, Application e Infrastructure |
| **DTOs Específicos** | CreateDTO, UpdateDTO e ResponseDTO separados |
| **Global Exception Handler** | Tratamento centralizado com ApiError padronizado |
| **Custom Exceptions** | EntityNotFoundException, DuplicateEntityException, BusinessValidationException |
| **Virtual Threads** | Habilitado para operações I/O-bound |
| **Flyway Migrations** | Versionamento de schema do banco |
| **OpenAPI 3.0** | Documentação completa com @ApiResponses |
| **Testes Unitários** | JUnit 5 + Mockito com @Nested |
| **SecurityService** | Verificação de roles e permissões |
| **Multi-tenancy** | Via TenantPersistenceEntityBase |

---

## Pré-requisitos

- **Java 25+** - [Download](https://adoptium.net/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Docker** - [Download](https://www.docker.com/products/docker-desktop)
- IDE recomendado: **IntelliJ IDEA**

---

## Quick Start

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/archbase-java-boilerplate.git
cd archbase-java-boilerplate
```

### 2. Inicie o PostgreSQL com Docker

```bash
docker-compose up -d postgres
```

### 3. Compile o projeto

```bash
mvn clean install
```

### 4. Execute a aplicação

```bash
cd archbase-boilerplate-rest
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Ou execute o JAR:

```bash
java -jar target/archbase-boilerplate-rest-1.0.0.jar --spring.profiles.active=dev
```

### 5. Acesse a aplicação

| Recurso | URL |
|---------|-----|
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Actuator Health | http://localhost:8080/actuator/health |
| H2 Console (dev) | http://localhost:8080/h2-console |

---

## Configuração

### Variáveis de Ambiente

```bash
# Profile
APP_PROFILE=dev

# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DATABASE=archbase_db
POSTGRES_USER=archbase
POSTGRES_PASSWORD=changeit

# JWT
ARCHBASE_JWT_SECRET=your-secret-key-change-in-production

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200

# Multi-tenancy
ARCHBASE_DEFAULT_TENANT_ID=a9f814d2-4dae-41f3-851b-8aa3d4706561
```

### Profiles

- **dev** - Desenvolvimento com PostgreSQL (`docker-compose up -d postgres`), `ddl-auto: update` e o
  seed que cria o primeiro administrador — `admin@archbase.com.br` / `admin`. Em qualquer outro
  perfil a senha é aleatória; para escolhê-la, defina `archbase.boilerplate.seed.admin.password`.
- **h2** - Desenvolvimento sem Docker, com H2 em memória
- **homolog** - Homologação com PostgreSQL e `ddl-auto: update`
- **prod** - Produção com PostgreSQL, Flyway como dono do schema e `ddl-auto: validate`

> A aplicação **recusa-se a subir** enquanto `ARCHBASE_JWT_SECRET` for o texto de exemplo: ele
> precisa ser Base64 válido. Gere o seu com `openssl rand -base64 48`.

```bash
# Desenvolvimento
java -jar app.jar --spring.profiles.active=dev

# Produção
java -jar app.jar --spring.profiles.active=prod
```

### Virtual Threads (Java 25)

Virtual Threads estão habilitados por padrão no `application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

---

## API Endpoints

### Produtos

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/produtos` | Criar produto |
| GET | `/api/v1/produtos/{id}` | Buscar por ID |
| GET | `/api/v1/produtos/sku/{sku}` | Buscar por SKU |
| GET | `/api/v1/produtos/findAll` | Listar paginado |
| GET | `/api/v1/produtos/ativos` | Listar ativos |
| GET | `/api/v1/produtos/categoria/{categoria}` | Listar por categoria |
| PUT | `/api/v1/produtos/{id}` | Atualizar produto |
| PATCH | `/api/v1/produtos/{id}/estoque` | Atualizar estoque |
| POST | `/api/v1/produtos/{id}/ativar` | Ativar produto |
| POST | `/api/v1/produtos/{id}/inativar` | Inativar produto |
| DELETE | `/api/v1/produtos/{id}` | Deletar produto |

### Exemplo de Requisição - Criar Produto

```bash
curl -X POST http://localhost:8080/api/v1/produtos \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Notebook Dell",
    "sku": "NOTE-001",
    "descricao": "Notebook Dell Inspiron 15",
    "preco": 3500.00,
    "categoria": "ELETRONICOS",
    "estoque": 10,
    "marca": "Dell"
  }'
```

### Exemplo de Requisição - Atualizar Produto (Parcial)

```bash
curl -X PUT http://localhost:8080/api/v1/produtos/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "preco": 3299.00,
    "estoque": 15
  }'
```

### Resposta de Erro Padrão

```json
{
  "status": "BAD_REQUEST",
  "timestamp": "18-04-2026 15:30:45",
  "message": "Erro de validação",
  "subErrors": [
    {
      "object": "produtoCreateDTO",
      "field": "nome",
      "rejectedValue": "",
      "message": "O nome é obrigatório"
    }
  ]
}
```

---

## Exceções Customizadas

| Exceção | HTTP Status | Uso |
|---------|-------------|-----|
| `EntityNotFoundException` | 404 | Entidade não encontrada |
| `DuplicateEntityException` | 409 | SKU ou campo único duplicado |
| `BusinessValidationException` | 422 | Regra de negócio violada |
| `BoilerplateException` | 400 | Exceção genérica de negócio |

### Exemplo de Uso

```java
ProdutoEntity entity = repository.findById(id)
    .orElseThrow(() -> new EntityNotFoundException("Produto", id));

if (repository.existsBySku(dto.getSku())) {
    throw new DuplicateEntityException("Produto", "SKU", dto.getSku());
}
```

---

## DTOs

### ProdutoCreateDTO

```java
@Data
@Builder
public class ProdutoCreateDTO {
    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser maior que zero")
    private BigDecimal preco;

    @NotNull(message = "A categoria é obrigatória")
    private CategoriaProduto categoria;

    private String descricao;
    private Integer estoque;
    private String sku;
    private String marca;
}
```

### ProdutoUpdateDTO

```java
@Data
@Builder
public class ProdutoUpdateDTO {
    // Todos os campos opcionais para update parcial
    private String nome;
    private BigDecimal preco;
    private CategoriaProduto categoria;
    private Boolean ativo;
    // ...
}
```

---

## Flyway Migrations

As migrations estão em `archbase-boilerplate-rest/src/main/resources/db/migration/`:

```
db/migration/
├── V1__create_produto_table.sql      # Criação da tabela produto
└── V2__add_audit_columns.sql         # Colunas de auditoria
```

### Criar Nova Migration

```sql
-- V3__add_new_feature.sql
ALTER TABLE produto ADD COLUMN nova_coluna VARCHAR(100);
```

---

## Testes

### Estrutura de Testes

```
src/test/java/
└── br/com/archbase/boilerplate/core/
    └── application/service/
        └── ProdutoServiceTest.java
```

### Executar Testes

```bash
mvn test
```

### Exemplo de Teste

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ProdutoService - Testes")
class ProdutoServiceTest {

    @Mock
    private ProdutoJpaRepository repository;

    @InjectMocks
    private ProdutoService service;

    @Nested
    @DisplayName("Método criar")
    class CriarTests {

        @Test
        @DisplayName("Deve criar produto com dados válidos")
        void deveCriarProdutoComDadosValidos() {
            // Given
            when(repository.existsBySku(anyString())).thenReturn(false);
            when(repository.save(any())).thenReturn(produtoEntity);

            // When
            ProdutoDTO result = service.criar(createDTO);

            // Then
            assertThat(result).isNotNull();
            verify(repository).save(any());
        }
    }
}
```

---

## SecurityService

O `SecurityService` fornece métodos para verificação de roles e permissões:

```java
@Service
@RequiredArgsConstructor
public class MyService {

    private final SecurityService securityService;

    public void myMethod() {
        // Verifica se é admin
        if (securityService.isAdmin()) {
            // Lógica de admin
        }

        // Verifica role específica
        if (securityService.hasRole("SUPERVISOR")) {
            // Lógica de supervisor
        }

        // Verifica se é dono do recurso ou admin
        if (securityService.isOwnerOrAdmin(resourceOwnerId)) {
            // Permite acesso
        }

        // Obtém dados do usuário
        String userId = securityService.getCurrentUserId();
        String tenantId = securityService.getCurrentTenantId();
    }
}
```

---

## Makefile

Comandos úteis disponíveis:

```bash
make build          # Compila o projeto
make run            # Executa com profile dev
make test           # Executa testes
make clean          # Limpa build
make docker-up      # Sobe containers Docker
make docker-down    # Para containers Docker
```

---

## Docker

### PostgreSQL apenas

```bash
docker-compose up -d postgres
```

### Todos os serviços

```bash
docker-compose up -d
```

### Parar serviços

```bash
docker-compose down
```

---

## Boas Práticas

### Repository Pattern

```java
@Repository
public interface ExampleJpaRepository extends ArchbaseCommonJpaRepository<ExampleEntity, String, Long> {
    // Queries simples aqui, queries complexas no Adapter com QueryDSL
}
```

### Entity Pattern

```java
@Entity
@Table(name = "example", indexes = {
    @Index(name = "idx_example_nome", columnList = "nome")
})
@Getter
@Setter
public class ExampleEntity extends TenantPersistenceEntityBase {
    @Column(name = "nome", nullable = false)
    private String nome;
}
```

### Controller Pattern

```java
@RestController
@RequestMapping("/api/v1/examples")
@Tag(name = "Examples", description = "Gerenciamento de Examples")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Validated
public class ExampleController {

    @PostMapping
    @Operation(summary = "Criar novo example")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<ExampleDTO> criar(@Valid @RequestBody ExampleCreateDTO dto) {
        // Sem try-catch - GlobalExceptionHandler trata as exceções
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }
}
```

---

## Documentação da API

A documentação interativa está disponível via Swagger UI:

```
http://localhost:8080/swagger-ui/index.html
```

Especificação OpenAPI:

```
http://localhost:8080/v3/api-docs
```

---

## Contribuindo

Contribuições são bem-vindas! Por favor:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

---

## Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

```
MIT License

Copyright (c) 2024 Edson Martins

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Autor

**Edson Martins**

- GitHub: [@edsonmartins](https://github.com/edsonmartins)

---

## Agradecimentos

- [Archbase App Framework](https://github.com/edsonmartins/archbase-app-framework) - Framework DDD para Java
- [Spring Boot](https://spring.io/projects/spring-boot) - Framework Spring
- [QueryDSL](https://querydsl.com/) - Queries tipo-safe para Java
- [MapStruct](https://mapstruct.org/) - Mapeamento de objetos Java
- [Flyway](https://flywaydb.org/) - Migrations de banco de dados
