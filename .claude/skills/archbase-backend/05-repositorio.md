# 05. Repository Pattern

Repositories JPA seguindo o padrão ArchbaseCommonJpaRepository.

---

## Conceito

**CRÍTICO**: Repository deve:
- Estender `ArchbaseCommonJpaRepository<Entity, ID, Long>`
- **SEM** métodos query customizados (findByXxx)
- Fornecer apenas CRUD básico
- Queries complexas vão no Adapter com QueryDSL

---

## Repository Básico

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.ExampleEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ExampleJpaRepository extends ArchbaseCommonJpaRepository<ExampleEntity, String, Long> {
    // SEM métodos customizados - todas as queries via QueryDSL no adapter
}
```

---

## Repository com Consultas Simples

**ATENÇÃO**: Apenas queries simples que não podem ser feitas via QueryDSL:

```java
@Repository
public interface ProdutoJpaRepository extends ArchbaseCommonJpaRepository<ProdutoEntity, String, Long> {

    // Permitido: busca por SKU (único, simples)
    Optional<ProdutoEntity> findBySku(String sku);

    // Permitido: verificar existência
    boolean existsBySkuAndTenantId(String sku, String tenantId);

    // Permitido: buscar por categoria (field simples)
    List<ProdutoEntity> findByCategoriaAndTenantId(CategoriaProduto categoria, String tenantId);

    // Permitido: buscar ativos
    List<ProdutoEntity> findByAtivoTrueAndTenantId(String tenantId);
}
```

---

## Parâmetros de ArchbaseCommonJpaRepository

```java
ArchbaseCommonJpaRepository<Entity, ID, Version>
```

| Parâmetro | Tipo | Descrição | Exemplo |
|-----------|------|-----------|---------|
| `Entity` | Class | A classe da entidade JPA | `ExampleEntity` |
| `ID` | Class | Tipo do ID (String, Long, etc.) | `String` |
| `Version` | Class | Tipo da versão para locking | `Long` |

```java
// ID String (mais comum)
ArchbaseCommonJpaRepository<ExampleEntity, String, Long>

// ID Long
ArchbaseCommonJpaRepository<ProdutoEntity, Long, Long>

// ID Integer
ArchbaseCommonJpaRepository<CategoriaEntity, Integer, Long>
```

---

## Métodos Herdados Automaticamente

Ao estender `ArchbaseCommonJpaRepository`, você herda:

```java
// CRUD básico
save(Entity entity)              // Salvar (insert ou update)
saveAll(Iterable<Entity>)        // Salvar em lote
findById(ID id)                  // Buscar por ID
findAll()                        // Buscar todos
findAllById(Iterable<ID>)        // Buscar por IDs
existsById(ID id)                // Verificar existência
count()                          // Contar total
deleteById(ID id)                // Deletar por ID
delete(Entity entity)            // Deletar entidade
deleteAll()                      // Deletar todos

// Paginação
findAll(Pageable pageable)       // Buscar paginado
findAll(Sort sort)               // Buscar ordenado

// Flush e refresh
flush()                          // Sincronizar com banco
saveAndFlush(Entity entity)      // Salvar e sincronizar
```

---

## Repositórios no Contexto Hexagonal

```
infrastructure/output/persistence/repository/
├── ExampleJpaRepository.java         # Repository interface
└── ProdutoJpaRepository.java         # Repository interface

infrastructure/output/persistence/adapter/
├── ExamplePersistenceAdapter.java    # Implementa Port + QueryDSL
└── ProdutoPersistenceAdapter.java    # Implementa Port + QueryDSL
```

---

## Exemplo de Repository com ID Long

```java
@Entity
@Table(name = "CATEGORIA")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "ID_CATEGORIA"))
})
public class CategoriaEntity extends TenantPersistenceEntityBase {
    // ID é Long, gerado automaticamente
}

@Repository
public interface CategoriaJpaRepository extends ArchbaseCommonJpaRepository<CategoriaEntity, Long, Long> {
    // SEM métodos customizados
}
```

---

## Quando Adicionar Métodos no Repository

**PERMITIDO** (queries simples, únicas):
```java
// Busca por campo único
Optional<Entity> findByCodigo(String codigo);

// Verificação de existência
boolean existsByCodigoAndTenantId(String codigo, String tenantId);

// Busca por enum
List<Entity> findByStatusAndTenantId(Status status, String tenantId);
```

**PROIBIDO** (queries complexas):
```java
// ERRADO - Use QueryDSL no adapter
List<Entity> findByNomeContainingAndCategoriaAndStatusOrderByNomeAsc(
    String nome, Categoria categoria, Status status);

// ERRADO - Use QueryDSL no adapter
List<Entity> findByDataCriacaoBetweenAndTenantId(LocalDateTime inicio, LocalDateTime fim, String tenantId);

// ERRADO - Use QueryDSL no adapter
@Entity
@Query("SELECT e FROM ExampleEntity e WHERE ...")
List<Entity> buscarComplexa(String param);
```

---

## Configuração do Repository

```java
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "br.com.empresa.projeto.core.infrastructure.output.persistence.repository"
    },
    repositoryBaseClass = CommonArchbaseJpaRepository.class
)
public class JpaConfig {
    // Configuração adicional se necessário
}
```

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **SEM query methods** | Todas as queries complexas no Adapter com QueryDSL |
| **ArchbaseCommonJpaRepository** | Extender sempre com 3 parâmetros |
| **@Repository** | Anotar com @Repository |
| **ID correto** | Usar `String` ou `Long` conforme necessidade |
| **Tenant-aware** | Queries sempre filtram por tenantId automaticamente |

---

**CRÍTICO**: Para queries complexas (joins, ordenações dinâmicas, filtros múltiplos), use sempre QueryDSL no PersistenceAdapter, nunca no Repository.
