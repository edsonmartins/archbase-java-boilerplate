# 05. Repository Pattern

Repositories JPA seguindo o padrão ArchbaseCommonJpaRepository.

---

## Conceito

**CRÍTICO**: Repository deve:
- Estender `ArchbaseCommonJpaRepository<Entity, ID, Long>`
- Ser anotado com `@Repository`
- Ter, no máximo, query methods simples do Spring Data (findBySku, existsBySku, ...)
- Deixar queries complexas (joins, agregações, filtros múltiplos) para o Adapter com QueryDSL

O `ProdutoJpaRepository` do boilerplate **tem** query methods simples. Esse é o padrão real
do código hoje.

---

## Repository Real: ProdutoJpaRepository

```java
package br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository;

import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.ProdutoEntity;
import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoJpaRepository extends ArchbaseCommonJpaRepository<ProdutoEntity, String, Long> {

    Optional<ProdutoEntity> findBySku(String sku);

    boolean existsBySku(String sku);

    List<ProdutoEntity> findByCategoria(CategoriaProduto categoria);

    List<ProdutoEntity> findByAtivoTrue();
}
```

**Sem `tenantId` nos métodos**: a entidade estende `TenantPersistenceEntityBase`, cujo campo
`tenantId` é anotado com `@TenantId` do Hibernate. O Hibernate aplica o filtro de tenant nas
queries automaticamente, então `existsBySku(sku)` já vale para o tenant corrente.

---

## Parâmetros de ArchbaseCommonJpaRepository

```java
ArchbaseCommonJpaRepository<Entity, ID, N extends Number & Comparable<N>>
```

| Parâmetro | Tipo | Descrição | Exemplo (Produto) |
|-----------|------|-----------|-------------------|
| `Entity` | Class | A classe da entidade JPA | `ProdutoEntity` |
| `ID` | Class | Tipo do ID (a base usa `String`, UUID) | `String` |
| `N` | Class | Tipo numérico (usado pelo contrato base) | `Long` |

---

## Métodos Herdados

`ArchbaseCommonJpaRepository` estende `JpaSpecificationExecutor` e o contrato `Repository` do
archbase (que traz o CRUD do Spring Data). Os métodos abaixo são usados no código real:

```java
save(entity)                       // insert ou update
findById(id)                       // Optional
findAll(Pageable pageable)         // paginado
findAllById(Iterable<ID> ids)
existsById(id)
deleteById(id)
findAll(String filter, Pageable)   // filtro RSQL (usado em ProdutoPersistenceAdapter.findWithFilter)
```

Também expõe utilitários QueryDSL (confirmados no jar 3.2.2): `query(Function<JPAQuery<?>, O>)`,
`update(Consumer<JPAUpdateClause>)`, `deleteWhere(Predicate)`, `findOne(JPQLQuery)`,
`findAll(JPQLQuery, Pageable)`.

---

## Configuração (real)

Em `ArchbaseBoilerplateApplication` (módulo rest):

```java
@EnableJpaRepositories(
        basePackages = {
            "br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository"
        },
        repositoryBaseClass = CommonArchbaseJpaRepository.class
)
@EntityScan(basePackages = {
        "br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity",
        "br.com.archbase.ddd.domain.entity"
})
```

`CommonArchbaseJpaRepository` é a implementação base (de `br.com.archbase.ddd.infraestructure.persistence.jpa.repository`).

---

## Estrutura

```
infrastructure/output/persistence/
├── repository/ProdutoJpaRepository.java
├── adapter/ProdutoPersistenceAdapter.java   # Implementa ProdutoPersistencePort + QueryDSL
├── mapper/ProdutoPersistenceMapper.java
└── entity/ProdutoEntity.java
```

---

## Quando Adicionar Métodos no Repository

**Usado hoje** (queries simples, derivadas do nome):
```java
Optional<ProdutoEntity> findBySku(String sku);
boolean existsBySku(String sku);
List<ProdutoEntity> findByCategoria(CategoriaProduto categoria);
List<ProdutoEntity> findByAtivoTrue();
```

**Evite** (nome gigante, vira ilegível; use QueryDSL no adapter, como `findByPrecoEntre`):
```java
List<ProdutoEntity> findByNomeContainingAndCategoriaAndAtivoOrderByNomeAsc(...);
```

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **ArchbaseCommonJpaRepository** | Estender sempre com 3 parâmetros |
| **@Repository** | Anotar com @Repository |
| **Query methods simples** | Permitidos (findBySku, existsBySku, findByAtivoTrue) |
| **Queries complexas** | QueryDSL no Adapter (faixa de preço, agregações) |
| **Tenant** | Filtro automático via `@TenantId` da entidade base; não passar tenantId |

---

**IMPORTANTE**: Hoje o `ProdutoService` injeta o `ProdutoJpaRepository` diretamente (não passa pelo Port/Adapter). Veja 06-port.md e 08-service.md.
