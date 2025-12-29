# Archbase Java Boilerplate

> Boilerplate para criação de projetos backend Java com Spring Boot 3 e Archbase Framework v2.0

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Archbase](https://img.shields.io/badge/Archbase-2.0.0-blue.svg)](https://github.com/archbase-framework)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Características

- **[Spring Boot 3.5.6](https://spring.io/projects/spring-boot)** - Framework web com suporte a Jakarta EE
- **[Java 17](https://openjdk.org/projects/jdk/17/)** - LTS com suporte extendido
- **[Archbase Framework 2.0.0](https://github.com/archbase-framework)** - Framework DDD brasileiro completo
- **[QueryDSL 5.1.0](https://querydsl.com/)** - Queries tipo-safe com JPA
- **Arquitetura Hexagonal** - Ports e Adapters (limpa e testável)
- **Multi-tenancy** - Suporte nativo a múltiplos tenants
- **PostgreSQL 16** - Banco de dados principal
- **H2** - Para desenvolvimento e testes
- **Docker Compose** - Infraestrutura local completa
- **OpenAPI/Swagger** - Documentação de API integrada
- **MapStruct** - Mapeamento de objetos eficiente
- **Lombok** - Redução de boilerplate

---

## Stack Tecnológico

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| Java | 17 | LTS |
| Spring Boot | 3.5.6 | Framework principal |
| Archbase Framework | 2.0.0 | DDD, Security, Multitenancy |
| QueryDSL | 5.1.0 | Queries tipo-safe |
| PostgreSQL | 16 | Banco de dados |
| H2 | 2.2.224 | Desenvolvimento |
| MapStruct | 1.5.5.Final | Mapeamento |
| Lombok | 1.18.34 | Boilerplate |
| Springdoc OpenAPI | 2.5.0 | Documentação |
| Undertow | - | Servlet container |

---

## Estrutura do Projeto

```
archbase-java-boilerplate/
├── archbase-boilerplate-core/     # Domínio, Aplicação e Persistência
│   ├── domain/                     # Entities, DTOs, Enums
│   ├── application/               # Services, ports, mappers
│   └── infrastructure/            # JPA entities, repositories, adapters
│
└── archbase-boilerplate-rest/      # REST API
    └── infrastructure/input/rest/ # Controllers REST
```

```
core/src/main/java/br/com/archbase/boilerplate/
├── domain/                                  # DOMÍNIO (DDD)
│   ├── entity/                              # Domain Objects
│   └── enums/                               # Enums do domínio
│
├── application/                             # APLICAÇÃO
│   ├── dto/                                 # DTOs
│   ├── mapper/                              # MapStruct mappers
│   └── service/                             # Services
│
└── infrastructure/output/persistence/        # INFRAESTRUTURA
    ├── entity/                              # JPA Entities
    ├── repository/                          # JPA Repositories
    └── adapter/                             # QueryDSL Adapters
```

---

## Pré-requisitos

- **Java 17+** - [Download](https://adoptium.net/)
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
mvn spring-boot:run
```

Ou execute o JAR:

```bash
java -jar target/archbase-boilerplate-rest-1.0.0.jar
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
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/archbase
DATABASE_USER=archbase
DATABASE_PASSWORD=archbase

# JWT
ARCHBASE_JWT_SECRET=your-secret-key-change-in-production

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
```

### Profiles

- **dev** - Desenvolvimento com H2 em memória
- **default** - Produção com PostgreSQL

```bash
# Desenvolvimento
java -jar app.jar --spring.profiles.active=dev

# Produção
java -jar app.jar
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

## API Endpoints

### Produtos

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/produtos` | Criar produto |
| GET | `/api/v1/produtos/{id}` | Buscar por ID |
| GET | `/api/v1/produtos/findAll` | Listar paginado |
| GET | `/api/v1/produtos/ativos` | Listar ativos |
| GET | `/api/v1/produtos/categoria/{categoria}` | Listar por categoria |
| PUT | `/api/v1/produtos/{id}` | Atualizar produto |
| DELETE | `/api/v1/produtos/{id}` | Deletar produto |

### Exemplo de Requisição

```bash
# Criar produto
curl -X POST http://localhost:8080/api/v1/produtos \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Notebook Dell",
    "sku": "NOTE-001",
    "descricao": "Notebook Dell Inspiron 15",
    "preco": 3500.00,
    "categoria": "ELETRONICOS",
    "estoque": 10,
    "ativo": true
  }'
```

---

## Padrões Arquiteturais

### Repository Pattern

```java
@Repository
public interface ExampleJpaRepository extends ArchbaseCommonJpaRepository<ExampleEntity, String, Long> {
    // SEM métodos customizados - queries via QueryDSL no adapter
}
```

**CRÍTICO**: Sempre use `ArchbaseCommonJpaRepository<Entity, ID, Long>` como base.

### Entity Pattern

```java
@Entity
@Table(name = "EXAMPLE")
@Getter
@Setter
public class ExampleEntity extends TenantPersistenceEntityBase {
    @Column(name = "NOME")
    private String nome;
}
```

**CRÍTICO**: Entities devem estender `TenantPersistenceEntityBase` para multi-tenancy.

### Security

```java
import br.com.archbase.security.annotations.RequireRole;

@RestController
public class ExampleController {

    @RequireRole({"ADMIN", "SUPERVISOR"})
    public ResponseEntity<ExampleDTO> create() {
        // Apenas ADMIN e SUPERVISOR
    }
}
```

**CRÍTICO**: Use anotações Archbase (`@RequireRole`), NUNCA Spring Security (`@PreAuthorize`).

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

## Desenvolvimento

### Gerar classes QueryDSL

```bash
mvn clean process-sources
```

### Executar testes

```bash
mvn test
```

### Build para produção

```bash
mvn clean package -DskipTests
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

- [Archbase Framework](https://github.com/archbase-framework) - Framework DDD para Java
- [Spring Boot](https://spring.io/projects/spring-boot) - Framework Spring
- [QueryDSL](https://querydsl.com/) - Queries tipo-safe para Java
- [MapStruct](https://mapstruct.org/) - Mapeamento de objetos Java
