# Archbase Backend - Skill de Referência

Este documento contém a referência completa para desenvolvimento backend Java com Spring Boot 3 e Archbase Framework v2.0.

**IMPORTANTE**: Documentação baseada em Archbase Framework 2.0.0, Spring Boot 3.5.6, Java 17.

---

## Índice de Arquivos

| Arquivo | Conteúdo | Linhas |
|---------|----------|--------|
| **01-projeto.md** | Estrutura hexagonal, pom.xml, application.yml, configurações | ~250 |
| **02-dominio.md** | Domain Objects (domínio puro sem dependências) | ~200 |
| **03-dto.md** | DTOs, validações, Bean Validation | ~200 |
| **04-entidade.md** | Entity Pattern, TenantPersistenceEntityBase, JPA | ~250 |
| **05-repositorio.md** | Repository Pattern, ArchbaseCommonJpaRepository | ~150 |
| **06-port.md** | Ports (hexagonal), interfaces entrada/saída | ~200 |
| **07-adapter.md** | Adapters, QueryDSL, implementação ports | ~300 |
| **08-service.md** | Services, lógica negócio, injeção ports | ~250 |
| **09-controller.md** | Controllers REST, OpenAPI/Swagger | ~250 |
| **10-mapper.md** | MapStruct, conversões Entity-DTO-Domain | ~200 |
| **11-querydsl.md** | Queries complexas, Q classes, joins | ~300 |
| **12-seguranca.md** | @RequireRole, @RequireProfile, @RequirePersona | ~300 |
| **13-crud-completo.md** | Exemplo completo passo a passo CRUD | ~500 |

---

## Comandos Mais Usados

### Maven
```bash
# Compilar
mvn clean compile

# Gerar Q classes QueryDSL
mvn clean process-sources

# Empacotar
mvn clean package

# Executar com profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Repository Pattern
```java
@Repository
public interface ExampleJpaRepository extends ArchbaseCommonJpaRepository<ExampleEntity, String, Long> {
    // SEM métodos customizados
}
```

### Entity Pattern
```java
@Entity
@Table(name = "TABELA")
@Getter
@Setter
public class ExampleEntity extends TenantPersistenceEntityBase {
    @Column(name = "CAMPO")
    private String campo;
}
```

### QueryDSL (no Adapter)
```java
QExampleEntity entity = QExampleEntity.exampleEntity;
return queryFactory
    .selectFrom(entity)
    .where(entity.tenantId.eq(tenantId))
    .fetch();
```

### Segurança Archbase
```java
@RequireRole({"ADMIN", "SUPERVISOR"})
@RequireProfile("MANAGER")
public ResponseEntity<ExampleDTO> create() { }
```

---

## Correções Críticas (ERRADO → CORRETO)

| Errado | Correto |
|--------|---------|
| `JpaRepository<Entity, Long>` | `ArchbaseCommonJpaRepository<Entity, String, Long>` |
| `extends EntityBase` custom | `extends TenantPersistenceEntityBase` |
| `@Builder` em entidades | Usar construtor manual |
| `findByXxx()` no repository | QueryDSL no adapter |
| `@PreAuthorize` do Spring | `@RequireRole` / `@RequireProfile` / `@RequirePersona` |
| Query methods no repository | Port + Adapter com QueryDSL |
| `@TableGenerator` | ID gerado manualmente ou UUID |
| Lógica no controller | Service com port de entrada |
| DTO na entidade | Domain Object separado |
| Mapper manual | MapStruct `@Mapper(componentModel = "spring")` |

---

## Como Usar

Para ver o conteúdo detalhado de cada área, consulte os arquivos numerados:

- **Precisa configurar projeto?** → `01-projeto.md`
- **Precisa criar Domain Object?** → `02-dominio.md`
- **Precisa criar DTO?** → `03-dto.md`
- **Precisa criar Entity?** → `04-entidade.md`
- **Precisa criar Repository?** → `05-repositorio.md`
- **Precisa criar Port?** → `06-port.md`
- **Precisa criar Adapter?** → `07-adapter.md`
- **Precisa criar Service?** → `08-service.md`
- **Precisa criar Controller?** → `09-controller.md`
- **Precisa criar Mapper?** → `10-mapper.md`
- **Precisa fazer queries?** → `11-querydsl.md`
- **Precisa adicionar segurança?** → `12-seguranca.md`
- **Precisa de CRUD completo?** → `13-crud-completo.md`

---

Versão: 2.0.0
Data: 2024-12-28
Framework: Archbase 2.0.0, Spring Boot 3.5.6, Java 17
