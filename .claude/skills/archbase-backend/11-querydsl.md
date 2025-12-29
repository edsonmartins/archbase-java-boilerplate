# 11. QueryDSL

Queries tipo-safe usando JPAQueryFactory no Adapter.

---

## Conceito

**CRÍTICO**: QueryDSL deve:
- Ser usado no Persistence Adapter
- Usar JPAQueryFactory injetado
- Usar classes Q geradas automaticamente
- Retornar Domain Objects (mapper.toDomain)

---

## Configuração JPAQueryFactory

```java
@Configuration
public class QueryDSLConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
```

No Adapter:
```java
@Component
@RequiredArgsConstructor
public class ExamplePersistenceAdapter {

    private final JPAQueryFactory queryFactory;  // Injetado automaticamente

    // ...
}
```

---

## Query Básica

```java
QExampleEntity entity = QExampleEntity.exampleEntity;

List<Example> result = queryFactory
        .selectFrom(entity)
        .where(entity.tenantId.eq(tenantId))
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .toList();
```

---

## Query com Filtros Opcionais (BooleanBuilder)

```java
public List<Example> buscarComFiltros(String nome, Boolean ativo, String tenantId) {
    QExampleEntity entity = QExampleEntity.exampleEntity;
    BooleanBuilder builder = new BooleanBuilder();

    // Filtro obrigatório
    builder.and(entity.tenantId.eq(tenantId));

    // Filtros opcionais
    if (nome != null && !nome.isBlank()) {
        builder.and(entity.nome.containsIgnoreCase(nome));
    }

    if (ativo != null) {
        builder.and(entity.ativo.eq(ativo));
    }

    return queryFactory
            .selectFrom(entity)
            .where(builder)
            .orderBy(entity.nome.asc())
            .fetch()
            .stream()
            .map(mapper::toDomain)
            .toList();
}
```

---

## Query com Between e Data

```java
public List<Example> buscarPorDataRange(
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        String tenantId) {

    QExampleEntity entity = QExampleEntity.exampleEntity;

    return queryFactory
            .selectFrom(entity)
            .where(
                entity.tenantId.eq(tenantId)
                .and(entity.createEntityDate.between(dataInicio, dataFim))
            )
            .orderBy(entity.createEntityDate.desc())
            .fetch()
            .stream()
            .map(mapper::toDomain)
            .toList();
}
```

---

## Query com IN (Lista)

```java
public List<Example> buscarPorCategorias(List<Categoria> categorias, String tenantId) {
    QExampleEntity entity = QExampleEntity.exampleEntity;

    return queryFactory
            .selectFrom(entity)
            .where(
                entity.tenantId.eq(tenantId)
                .and(entity.categoria.in(categorias))
            )
            .fetch()
            .stream()
            .map(mapper::toDomain)
            .toList();
}
```

---

## Query com Joins

```java
// Inner Join
public List<Pedido> buscarPedidosComCliente(String clienteId) {
    QPedidoEntity pedido = QPedidoEntity.pedidoEntity;
    QClienteEntity cliente = QClienteEntity.clienteEntity;

    return queryFactory
            .selectFrom(pedido)
            .innerJoin(pedido.cliente, cliente)
            .where(cliente.id.eq(clienteId))
            .fetch()
            .stream()
            .map(mapper::toDomain)
            .toList();
}

// Left Join
public List<Pedido> buscarPedidosComItens(String tenantId) {
    QPedidoEntity pedido = QPedidoEntity.pedidoEntity;
    QPedidoItemEntity item = QPedidoItemEntity.pedidoItemEntity;

    return queryFactory
            .selectFrom(pedido)
            .leftJoin(pedido.itens, item).fetchJoin()
            .where(pedido.tenantId.eq(tenantId))
            .distinct()
            .fetch()
            .stream()
            .map(mapper::toDomain)
            .toList();
}
```

---

## Query com FetchJoin (Performance)

```java
public List<Produto> buscarProdutosComCategoria(String tenantId) {
    QProdutoEntity produto = QProdutoEntity.produtoEntity;

    return queryFactory
            .selectFrom(produto)
            .innerJoin(produto.categoria).fetchJoin()  // Evita N+1
            .where(produto.tenantId.eq(tenantId))
            .fetch()
            .stream()
            .map(mapper::toDomain)
            .toList();
}
```

---

## Query Paginada

```java
public Page<Example> buscarPaginado(int page, int size, String tenantId) {
    QExampleEntity entity = QExampleEntity.exampleEntity;

    List<Example> content = queryFactory
            .selectFrom(entity)
            .where(entity.tenantId.eq(tenantId))
            .orderBy(entity.nome.asc())
            .offset(page * size)
            .limit(size)
            .fetch()
            .stream()
            .map(mapper::toDomain)
            .toList();

    // Contar total
    long total = queryFactory
            .select(entity.count())
            .from(entity)
            .where(entity.tenantId.eq(tenantId))
            .fetchOne();

    return new PageImpl<>(content, PageRequest.of(page, size), total);
}
```

---

## Query com Projeção

```java
public List<ResumoDTO> buscarResumo(String tenantId) {
    QExampleEntity entity = QExampleEntity.exampleEntity;

    return queryFactory
            .select(
                entity.id,
                entity.nome,
                entity.valor
            )
            .from(entity)
            .where(entity.tenantId.eq(tenantId))
            .fetch()
            .stream()
            .map(tuple -> new ResumoDTO(
                tuple.get(entity.id),
                tuple.get(entity.nome),
                tuple.get(entity.valor)
            ))
            .toList();
}
```

---

## Query com Agregação

```java
public BigDecimal somarValores(String tenantId) {
    QExampleEntity entity = QExampleEntity.exampleEntity;

    return queryFactory
            .select(entity.valor.sum())
            .from(entity)
            .where(entity.tenantId.eq(tenantId))
            .fetchOne();
}

public EstatisticasDTO buscarEstatisticas(String tenantId) {
    QExampleEntity entity = QExampleEntity.exampleEntity;

    Tuple result = queryFactory
            .select(
                entity.count(),
                entity.valor.sum(),
                entity.valor.avg(),
                entity.valor.max(),
                entity.valor.min()
            )
            .from(entity)
            .where(entity.tenantId.eq(tenantId))
            .fetchOne();

    return new EstatisticasDTO(
        result.get(entity.count()),
        result.get(entity.valor.sum()),
        result.get(entity.valor.avg()),
        result.get(entity.valor.max()),
        result.get(entity.valor.min())
    );
}
```

---

## Query com GroupBy

```java
public List<ContagemPorCategoria> contarPorCategoria(String tenantId) {
    QExampleEntity entity = QExampleEntity.exampleEntity;

    return queryFactory
            .select(
                entity.categoria,
                entity.count()
            )
            .from(entity)
            .where(entity.tenantId.eq(tenantId))
            .groupBy(entity.categoria)
            .fetch()
            .stream()
            .map(tuple -> new ContagemPorCategoria(
                tuple.get(entity.categoria),
                tuple.get(entity.count())
            ))
            .toList();
}
```

---

## Query com Case (When/Then)

```java
public List<ExampleDTO> buscarComStatusCalculado(String tenantId) {
    QExampleEntity entity = QExampleEntity.exampleEntity;

    return queryFactory
            .selectFrom(entity)
            .where(entity.tenantId.eq(tenantId))
            .fetch()
            .stream()
            .map(e -> {
                ExampleDTO dto = mapper.toDTO(mapper.toDomain(e));
                // Calcular status baseado em regras
                if (e.getValor().compareTo(BigDecimal.valueOf(100)) > 0) {
                    dto.setStatus("ALTO");
                } else if (e.getValor().compareTo(BigDecimal.valueOf(50)) > 0) {
                    dto.setStatus("MEDIO");
                } else {
                    dto.setStatus("BAIXO");
                }
                return dto;
            })
            .toList();
}
```

---

## Query com Subquery

```java
public List<Example> buscarComSubquery(String tenantId) {
    QExampleEntity entity = QExampleEntity.exampleEntity;
    QPedidoEntity pedido = QPedidoEntity.pedidoEntity;

    return queryFactory
            .selectFrom(entity)
            .where(entity.tenantId.eq(tenantId)
                .and(entity.id.in(
                    JPAExpressions.select(pedido.exampleId)
                        .from(pedido)
                        .where(pedido.status.eq(Pedido.Status.CONCLUIDO))
                ))
            )
            .fetch()
            .stream()
            .map(mapper::toDomain)
            .toList();
}
```

---

## Tabela de Operações QueryDSL

| Operação | Sintaxe | Descrição |
|----------|---------|-----------|
| `eq` | `entity.campo.eq(valor)` | Igual |
| `ne` | `entity.campo.ne(valor)` | Diferente |
| `contains` | `entity.texto.contains("x")` | Contém |
| `startsWith` | `entity.texto.startsWith("x")` | Começa com |
| `endsWith` | `entity.texto.endsWith("x")` | Termina com |
| `gt` | `entity.numero.gt(10)` | Maior que |
| `goe` | `entity.numero.goe(10)` | Maior ou igual |
| `lt` | `entity.numero.lt(10)` | Menor que |
| `loe` | `entity.numero.loe(10)` | Menor ou igual |
| `between` | `entity.data.between(inicio, fim)` | Entre |
| `in` | `entity.status.in(lista)` | Em lista |
| `notIn` | `entity.status.notIn(lista)` | Não em lista |
| `isNull` | `entity.campo.isNull()` | É nulo |
| `isNotNull` | `entity.campo.isNotNull()` | Não é nulo |
| `like` | `entity.texto.like("%x%")` | Like SQL |
| `orderBy` | `.orderBy(entity.nome.asc())` | Ordenar |
| `distinct` | `.distinct()` | Remover duplicatas |

---

**CRÍTICO**: Queries complexas SEMPRE no Adapter com QueryDSL, JAMAS no Repository.
