---
name: archbase-backend
description: Referência para desenvolver backend Java com Spring Boot 4 e Archbase Framework 3.2.x neste boilerplate (CRUD de Produto como modelo). Use ao criar ou alterar projeto, domínio, DTO, entidade JPA, repository, port/adapter, service, controller REST, mapper MapStruct, queries QueryDSL/RSQL, segurança, migrations ou testes.
---

# Archbase Backend - Skill de Referência

Documenta **o que o boilerplate faz hoje**, usando o CRUD de `Produto` como modelo. Quando houver dúvida, o código (`archbase-boilerplate-core` e `archbase-boilerplate-rest`) é a fonte de verdade.

**Stack**: Archbase 3.2.2, Spring Boot 4.1.0, Java 25, Hibernate 7, QueryDSL (fork `io.github.openfeign.querydsl` 7.2), MapStruct 1.6.3, Lombok 1.18.46, springdoc 3.0.0.

**Pacotes**: `br.com.archbase.boilerplate.core` (domínio, aplicação, persistência) e `br.com.archbase.boilerplate.rest` (controller, config, filtros, seeds).

---

## Índice de Arquivos

| Arquivo | Conteúdo |
|---------|----------|
| **01-projeto.md** | Estrutura de módulos, poms, `application*.yml`, profiles, Flyway |
| **02-dominio.md** | `Produto` (domínio), enum, hierarquia de exceções |
| **03-dto.md** | DTOs de Produto e validações |
| **04-entidade.md** | `ProdutoEntity`, `TenantPersistenceEntityBase`, Q classes |
| **05-repositorio.md** | `ArchbaseCommonJpaRepository` e métodos de consulta simples |
| **06-port.md** | `ProdutoPersistencePort` (só `port/out`) e filtros RSQL |
| **07-adapter.md** | `ProdutoPersistenceAdapter`, QueryDSL, agregações |
| **08-service.md** | `ProdutoService` e testes de service |
| **09-controller.md** | `ProdutoController`, OpenAPI |
| **10-mapper.md** | `ProdutoPersistenceMapper` (MapStruct) |
| **11-querydsl.md** | `QueryDslConfig`, queries, RSQL e paginação |
| **12-seguranca.md** | `@RequireRole`, `ArchbaseRoleResolver` e proteções do archbase |
| **13-crud-completo.md** | Passo a passo de um CRUD completo |
| **14-infraestrutura.md** | Tratamento de erros, filtros de tenant, seeds, JWT, rate limit, testes |

---

## Fluxo real de uma requisição

```
ProdutoController -> ProdutoService -> ProdutoJpaRepository -> ProdutoEntity (toDTO/fromDTO)
```

`ProdutoPersistencePort`, `ProdutoPersistenceAdapter`, `ProdutoPersistenceMapper` e o domínio `Produto` existem como modelo hexagonal, mas hoje só são usados entre si. Não há camada `port/in`.

## Comandos

```bash
mvn clean compile           # compilar (gera as Q classes)
mvn clean package           # empacotar
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Há também um `Makefile` na raiz (ver `01-projeto.md`).

## Padrões que valem sempre

| Padrão | Observação |
|--------|-----------|
| Repository | `extends ArchbaseCommonJpaRepository<Entity, String, Long>`; query methods simples (`findBySku`) são aceitos, consultas complexas vão no adapter com QueryDSL |
| Entidade | `extends TenantPersistenceEntityBase`; `tenantId` é filtrado pelo `@TenantId`, não é preciso filtrar nas queries |
| Mapper | MapStruct `@Mapper(componentModel = "spring")` |
| Validação | `jakarta.validation` nos DTOs de entrada e `@Valid` no controller |
| Erros | exceções da hierarquia `BoilerplateException`, traduzidas pelo `RestExceptionHandler` |
| Segurança | `@RequireRole` só protege com um `ArchbaseRoleResolver` real (ver `12-seguranca.md`); `@SecurityRequirement` no controller é apenas documentação OpenAPI |

---

## Como Usar

- **Configurar projeto, poms, yml?** → `01-projeto.md`
- **Domínio, exceções?** → `02-dominio.md`
- **DTO?** → `03-dto.md`
- **Entidade JPA?** → `04-entidade.md`
- **Repository?** → `05-repositorio.md`
- **Port?** → `06-port.md`
- **Adapter?** → `07-adapter.md`
- **Service e testes?** → `08-service.md`
- **Controller?** → `09-controller.md`
- **Mapper?** → `10-mapper.md`
- **Queries?** → `11-querydsl.md`
- **Segurança?** → `12-seguranca.md`
- **CRUD novo do zero?** → `13-crud-completo.md`
- **Erros, tenant, seeds, JWT, rate limit?** → `14-infraestrutura.md`

---

Framework: Archbase 3.2.2, Spring Boot 4.1.0, Java 25
