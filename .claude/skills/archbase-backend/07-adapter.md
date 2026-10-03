# 07. Adapters (Hexagonal - Infraestrutura)

Adapters implementam os Ports de saída, usando QueryDSL para queries.

---

## Conceito

Adapter deve:
- Implementar um Port de saída (`ProdutoPersistencePort`)
- Usar `com.querydsl.jpa.impl.JPAQueryFactory` (bean fornecido por `QueryDslConfig`, módulo rest)
- Converter Entity <-> Domain via Mapper (`ProdutoPersistenceMapper`)
- Estar anotado com `@Component`, `@RequiredArgsConstructor`, `@Slf4j`

O `JPAQueryFactory` **não** é do archbase: vem de `io.github.openfeign.querydsl` (pacote `com.querydsl`).

---

## Adapter Real: ProdutoPersistenceAdapter

```java
package br.com.archbase.boilerplate.core.infrastructure.output.persistence.adapter;

import br.com.archbase.boilerplate.core.application.dto.ProdutoEstatisticasDTO;
import br.com.archbase.boilerplate.core.application.port.out.ProdutoPersistencePort;
import br.com.archbase.boilerplate.core.domain.entity.Produto;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.ProdutoEntity;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.QProdutoEntity;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.mapper.ProdutoPersistenceMapper;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository.ProdutoJpaRepository;
import br.com.archbase.query.rsql.jpa.SortUtils;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProdutoPersistenceAdapter implements ProdutoPersistencePort {

    private static final QProdutoEntity qProduto = QProdutoEntity.produtoEntity;

    private final ProdutoJpaRepository repository;
    private final JPAQueryFactory queryFactory;
    private final ProdutoPersistenceMapper mapper;

    @Override
    public Produto save(Produto produto) {
        ProdutoEntity saved = repository.save(mapper.toEntity(produto));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Produto> findBySku(String sku) {
        return repository.findBySku(sku).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> findByCategoria(String categoria) {
        return repository.findByCategoria(CategoriaProduto.valueOf(categoria)).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    // ...
}
```

Métodos simples delegam ao `ProdutoJpaRepository` (CRUD, `findBySku`, `existsBySku`,
`findByAtivoTrue`). Métodos de leitura levam `@Transactional(readOnly = true)`.

---

## Queries QueryDSL no Adapter

```java
@Override
@Transactional(readOnly = true)
public List<Produto> findByPrecoEntre(BigDecimal min, BigDecimal max) {
    List<ProdutoEntity> entities = queryFactory
            .selectFrom(qProduto)
            .where(qProduto.preco.between(min, max))
            .fetch();
    return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
}

@Override
@Transactional(readOnly = true)
public List<Produto> findByMarca(String marca) {
    return queryFactory.selectFrom(qProduto)
            .where(qProduto.marca.equalsIgnoreCase(marca))
            .fetch().stream().map(mapper::toDomain).collect(Collectors.toList());
}
```

Sem `qProduto.tenantId.eq(...)`: o filtro de tenant é aplicado pelo Hibernate (`@TenantId`).

---

## Agregação com Tuple (obterEstatisticas)

```java
Long totalProdutos = queryFactory
        .select(qProduto.count())
        .from(qProduto)
        .fetchOne();

Long estoqueTotal = queryFactory
        .select(qProduto.estoque.sumAggregate().longValue())
        .from(qProduto)
        .fetchOne();

Map<String, Long> produtosPorCategoria = new HashMap<>();
List<Tuple> categoriaResults = queryFactory
        .select(qProduto.categoria, qProduto.count())
        .from(qProduto)
        .groupBy(qProduto.categoria)
        .fetch();
for (Tuple tuple : categoriaResults) {
    CategoriaProduto cat = tuple.get(qProduto.categoria);
    Long count = tuple.get(qProduto.count());
    if (cat != null && count != null) {
        produtosPorCategoria.put(cat.name(), count);
    }
}
```

O resultado é montado em `ProdutoEstatisticasDTO` (nulos de contagem viram `0L`). Note que
`avg()` devolve `Double` e é convertido com `BigDecimal.valueOf`.

---

## Métodos do FindDataWithFilterQuery (RSQL + paginação)

```java
@Override
@Transactional(readOnly = true)
public Page<Produto> findAll(int page, int size, String[] sort) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
    Page<ProdutoEntity> result = repository.findAll(pageable);
    List<Produto> list = result.stream().map(mapper::toDomain).collect(Collectors.toList());
    return new PageProduto(list, pageable, result.getTotalElements());
}

@Override
@Transactional(readOnly = true)
public Page<Produto> findWithFilter(String filter, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<ProdutoEntity> result = repository.findAll(filter, pageable);   // filtro RSQL
    // ... mapper::toDomain ...
    return new PageProduto(list, pageable, result.getTotalElements());
}

@Override
@Transactional(readOnly = true)
public Produto findById(String id) {
    return repository.findById(id).map(mapper::toDomain).orElse(null);   // null se não achar
}
```

- `SortUtils` é `br.com.archbase.query.rsql.jpa.SortUtils` (`convertSortToJpa(String[])`).
- `PageProduto` é uma classe interna que estende `PageImpl<Produto>`.
- `findById` devolve `null` (contrato do `FindDataWithFilterQuery`), não `Optional`.

---

## Tabela de Operações QueryDSL

| Operação | QueryDSL | Descrição |
|----------|----------|-----------|
| `eq` | `qProduto.sku.eq(valor)` | Igual a |
| `between` | `qProduto.preco.between(min, max)` | Entre valores |
| `isTrue` / `isFalse` | `qProduto.ativo.isTrue()` | Booleano |
| `equalsIgnoreCase` | `qProduto.marca.equalsIgnoreCase(m)` | Texto, ignora caixa |
| `isNotNull` | `qProduto.marca.isNotNull()` | Não nulo |
| `count` / `min` / `max` / `avg` | `qProduto.preco.max()` | Agregações |
| `groupBy` | `.groupBy(qProduto.categoria)` | Agrupamento |
| `gt` / `goe` / `lt` / `loe` / `in` / `contains` | padrão QueryDSL | Demais operadores |

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **@Component** | Sempre anotar com @Component |
| **@RequiredArgsConstructor** | Injeção via construtor |
| **Q-class estática** | `private static final QProdutoEntity qProduto = QProdutoEntity.produtoEntity;` |
| **@Transactional(readOnly = true)** | Em leituras; escritas via repository já são transacionais |
| **Mapper** | Entity -> Domain com `mapper::toDomain` |
| **RSQL** | Delegar ao `repository.findAll(filter, pageable)` |

---

**IMPORTANTE**: Hoje o adapter não é injetado por nenhum Service (ver 06-port.md); o `ProdutoService` usa o repository direto.
