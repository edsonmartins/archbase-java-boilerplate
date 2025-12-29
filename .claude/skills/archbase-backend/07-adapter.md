# 07. Adapters (Hexagonal - Infraestrutura)

Adapters implementam os Ports de saída, usando QueryDSL para queries.

---

## Conceito

**CRÍTICO**: Adapter deve:
- Implementar um Port de saída
- Usar QueryDSL para queries (JPAQueryFactory)
- Converter Entity ↔ Domain via Mapper
- Estar anotado com `@Component`

---

## Adapter Básico

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.adapter;

import br.com.archbase.ddd.infraestructure.persistence.jpa.querydsl.JpaQueryFactory;
import br.com.empresa.projeto.core.application.port.out.ExamplePersistencePort;
import br.com.empresa.projeto.core.domain.entity.Example;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.QExampleEntity;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.ExampleEntity;
import br.com.empresa.projeto.core.infrastructure.output.persistence.mapper.ExampleMapper;
import br.com.empresa.projeto.core.infrastructure.output.persistence.repository.ExampleJpaRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamplePersistenceAdapter implements ExamplePersistencePort {

    private final ExampleJpaRepository repository;
    private final JPAQueryFactory queryFactory;
    private final ExampleMapper mapper;

    @Override
    public Example save(Example example) {
        log.debug("Salvando example: {}", example.getId());
        ExampleEntity entity = mapper.toEntity(example);
        ExampleEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Example> findById(String id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Example> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Example> findByTenantId(String tenantId) {
        QExampleEntity entity = QExampleEntity.exampleEntity;
        return queryFactory
                .selectFrom(entity)
                .where(entity.tenantId.eq(tenantId))
                .fetch()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        log.debug("Deletando example: {}", id);
        repository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }
}
```

---

## Adapter com QueryDSL Complexo

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.adapter;

import br.com.empresa.projeto.core.application.port.out.ProdutoPersistencePort;
import br.com.empresa.projeto.core.application.dto.ProdutoFilterDTO;
import br.com.empresa.projeto.core.domain.entity.Produto;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.*;
import br.com.empresa.projeto.core.infrastructure.output.persistence.repository.ProdutoJpaRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProdutoPersistenceAdapter implements ProdutoPersistencePort {

    private final ProdutoJpaRepository repository;
    private final JPAQueryFactory queryFactory;
    private final ProdutoMapper mapper;

    @Override
    public Page<Produto> findByFilter(ProdutoFilterDTO filter, Pageable pageable) {
        QProdutoEntity produto = QProdutoEntity.produtoEntity;
        QProdutoEntity p = produto; // alias

        BooleanBuilder builder = new BooleanBuilder();

        // Filtro por tenant (sempre aplicado)
        if (filter.getTenantId() != null) {
            builder.and(p.tenantId.eq(filter.getTenantId()));
        }

        // Filtro por nome (busca parcial)
        filter.getNome().ifPresent(nome ->
            builder.and(p.nome.containsIgnoreCase(nome))
        );

        // Filtro por SKU
        filter.getSku().ifPresent(sku ->
            builder.and(p.sku.containsIgnoreCase(sku))
        );

        // Filtro por categoria
        filter.getCategoria().ifPresent(categoria ->
            builder.and(p.categoria.eq(categoria))
        );

        // Filtro por ativo
        filter.getAtivo().ifPresent(ativo ->
            builder.and(p.ativo.eq(ativo))
        );

        // Filtro por faixa de preço
        filter.getPrecoMin().ifPresent(min ->
            builder.and(p.preco.goe(min))
        );
        filter.getPrecoMax().ifPresent(max ->
            builder.and(p.preco.loe(max))
        );

        // Filtro por data de criação
        filter.getDataCriacaoInicio().ifPresent(inicio ->
            builder.and(p.createEntityDate.goe(inicio))
        );
        filter.getDataCriacaoFim().ifPresent(fim ->
            builder.and(p.createEntityDate.loe(fim))
        );

        // Executa query
        var query = queryFactory
                .selectFrom(p)
                .where(builder)
                .orderBy(p.nome.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<Produto> content = query.fetch()
                .stream()
                .map(mapper::toDomain)
                .toList();

        // Conta total para paginação
        long total = queryFactory
                .select(p.count())
                .from(p)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<Produto> findBySku(String sku, String tenantId) {
        QProdutoEntity produto = QProdutoEntity.produtoEntity;
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(produto)
                        .where(produto.sku.eq(sku)
                                .and(produto.tenantId.eq(tenantId)))
                        .fetchOne()
            ).map(mapper::toDomain);
    }
}
```

---

## Adapter com Join

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.adapter;

import br.com.empresa.projeto.core.application.port.out.PedidoPersistencePort;
import br.com.empresa.projeto.core.domain.entity.Pedido;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.*;
import br.com.empresa.projeto.core.infrastructure.output.persistence.repository.PedidoJpaRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PedidoPersistenceAdapter implements PedidoPersistencePort {

    private final PedidoJpaRepository repository;
    private final JPAQueryFactory queryFactory;
    private final PedidoMapper mapper;

    @Override
    public List<Pedido> findByClienteId(String clienteId, String tenantId) {
        QPedidoEntity pedido = QPedidoEntity.pedidoEntity;
        QClienteEntity cliente = QClienteEntity.clienteEntity;

        return queryFactory
                .selectFrom(pedido)
                .innerJoin(pedido.cliente, cliente)
                .where(cliente.id.eq(clienteId)
                        .and(pedido.tenantId.eq(tenantId)))
                .fetch()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Pedido> findByIdWithItens(String id) {
        QPedidoEntity pedido = QPedidoEntity.pedidoEntity;
        QPedidoItemEntity item = QPedidoItemEntity.pedidoItemEntity;

        PedidoEntity entity = queryFactory
                .selectFrom(pedido)
                .leftJoin(pedido.itens, item).fetchJoin()
                .where(pedido.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(entity)
                .map(mapper::toDomain);
    }
}
```

---

## Adapter com Agregação

```java
@Override
public BigDecimal sumTotalByTenantId(String tenantId) {
    QPedidoEntity pedido = QPedidoEntity.pedidoEntity;
    return queryFactory
            .select(pedido.valorTotal.sum())
            .from(pedido)
            .where(pedido.tenantId.eq(tenantId))
            .fetchOne();
}

@Override
public List<ResumoVendasDTO> getResumoPorCategoria(String tenantId) {
    QPedidoEntity pedido = QPedidoEntity.pedidoEntity;

    return queryFactory
            .select(
                    pedido.categoria,
                    pedido.valorTotal.sum(),
                    pedido.count()
            )
            .from(pedido)
            .where(pedido.tenantId.eq(tenantId))
            .groupBy(pedido.categoria)
            .fetch()
            .stream()
            .map(tuple -> new ResumoVendasDTO(
                    tuple.get(pedido.categoria),
                    tuple.get(pedido.valorTotal.sum()),
                    tuple.get(pedido.count())
            ))
            .toList();
}
```

---

## Adapter com Batch Operations

```java
@Override
public List<Example> saveAll(List<Example> examples) {
    List<ExampleEntity> entities = examples.stream()
            .map(mapper::toEntity)
            .toList();
    List<ExampleEntity> saved = repository.saveAll(entities);
    return saved.stream()
            .map(mapper::toDomain)
            .toList();
}

@Override
@Transactional
public void deleteAllByIds(List<String> ids) {
    QExampleEntity entity = QExampleEntity.exampleEntity;
    queryFactory
            .delete(entity)
            .where(entity.id.in(ids))
            .execute();
}
```

---

## Tabela de Operações QueryDSL

| Operação | QueryDSL | Descrição |
|----------|----------|-----------|
| `eq` | `entity.campo.eq(valor)` | Igual a |
| `ne` | `entity.campo.ne(valor)` | Diferente de |
| `contains` / `startsWith` / `endsWith` | String parcial | Busca texto |
| `gt` / `goe` | `entity.numero.goe(valor)` | Maior que / Maior ou igual |
| `lt` / `loe` | `entity.numero.loe(valor)` | Menor que / Menor ou igual |
| `between` | `entity.data.between(inicio, fim)` | Entre valores |
| `in` | `entity.status.in(lista)` | Em lista |
| `isNull` / `isNotNull` | `entity.campo.isNull()` | É nulo |
| `like` | `entity.nome.like("%texto%")` | Like SQL |
| `orderBy` | `.orderBy(entity.nome.asc())` | Ordenação |
| `leftJoin` / `innerJoin` | `.join(entity.relacao)` | Join |

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **@Component** | Sempre anotar com @Component |
| **@RequiredArgsConstructor** | Injeção via construtor |
| **BooleanBuilder** | Para filtros dinâmicos/opcionais |
| **Stream().map()** | Converter Entity → Domain via mapper |
| **@Transactional** | Em operações de escrita |
| **Log debug** | Logar operações importantes |

---

**CRÍTICO**: NUNCA faça queries no Repository. Use QueryDSL no Adapter.
