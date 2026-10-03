# 11. QueryDSL

Queries type-safe usando `JPAQueryFactory` no Adapter.

---

## Conceito

- QueryDSL do projeto: `io.github.openfeign.querydsl` 7.2 (`querydsl-jpa` + `querydsl-apt` classifier `jakarta`), pacotes `com.querydsl.*`
- Usado no Persistence Adapter (`ProdutoPersistenceAdapter`) com `JPAQueryFactory`
- Classes Q (ex.: `QProdutoEntity`) geradas pelo annotation processor no build do módulo core
- Retorna Domain Objects (`mapper::toDomain`) ou DTOs de agregação

---

## Configuração JPAQueryFactory (real)

Em `archbase-boilerplate-rest`, `infrastructure/config/QueryDslConfig.java`:

```java
import com.querydsl.jpa.impl.JPAQueryFactory;

@Configuration
public class QueryDslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
```

Sem esse bean a aplicação não sobe (`NoSuchBeanDefinitionException ... JPAQueryFactory`), porque o
adapter o injeta.

No Adapter:
```java
private static final QProdutoEntity qProduto = QProdutoEntity.produtoEntity;
private final JPAQueryFactory queryFactory;
```

---

## Tenant

Não filtre por `tenantId` nas queries. A entidade base (`TenantPersistenceEntityBase`) marca o
campo com `@TenantId` do Hibernate, que aplica o filtro do tenant corrente nas consultas.

---

## Query Básica (real: findByPrecoEntre)

```java
List<ProdutoEntity> entities = queryFactory
        .selectFrom(qProduto)
        .where(qProduto.preco.between(min, max))
        .fetch();
return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
```

Outros exemplos reais: `qProduto.destaque.isTrue()` (findEmDestaque) e
`qProduto.marca.equalsIgnoreCase(marca)` (findByMarca).

---

## Agregações (real: obterEstatisticas)

```java
Long total = queryFactory.select(qProduto.count()).from(qProduto).fetchOne();

Long ativos = queryFactory.select(qProduto.count()).from(qProduto)
        .where(qProduto.ativo.isTrue())
        .fetchOne();

Double precoMedio = queryFactory.select(qProduto.preco.avg()).from(qProduto).fetchOne();
BigDecimal precoMinimo = queryFactory.select(qProduto.preco.min()).from(qProduto).fetchOne();
BigDecimal precoMaximo = queryFactory.select(qProduto.preco.max()).from(qProduto).fetchOne();

Long estoqueTotal = queryFactory
        .select(qProduto.estoque.sumAggregate().longValue())
        .from(qProduto)
        .fetchOne();
```

`avg()` retorna `Double` (converta com `BigDecimal.valueOf`); `fetchOne()` pode retornar `null`, então trate com `!= null ? x : 0L`.

---

## GroupBy com Tuple (real)

```java
import com.querydsl.core.Tuple;

List<Tuple> marcaResults = queryFactory
        .select(qProduto.marca, qProduto.count())
        .from(qProduto)
        .where(qProduto.marca.isNotNull())
        .groupBy(qProduto.marca)
        .fetch();

for (Tuple tuple : marcaResults) {
    String marca = tuple.get(qProduto.marca);
    Long count = tuple.get(qProduto.count());
    if (marca != null && count != null) {
        produtosPorMarca.put(marca, count);
    }
}
```

---

## Filtros Opcionais (BooleanBuilder) - padrão, ainda não usado no adapter

Padrão QueryDSL para filtros dinâmicos, aplicado a campos reais de `ProdutoEntity`:

```java
BooleanBuilder builder = new BooleanBuilder();
if (nome != null && !nome.isBlank()) builder.and(qProduto.nome.containsIgnoreCase(nome));
if (categoria != null) builder.and(qProduto.categoria.eq(categoria));
if (ativo != null) builder.and(qProduto.ativo.eq(ativo));

List<ProdutoEntity> result = queryFactory.selectFrom(qProduto)
        .where(builder)
        .orderBy(qProduto.nome.asc())
        .fetch();
```

Para filtros vindos do frontend, o caminho usado hoje é RSQL: `findWithFilter(filter, page, size)`
do `FindDataWithFilterQuery`, que delega a `repository.findAll(filter, pageable)`.

---

## Paginação e Ordenação (real: via repository + SortUtils)

O adapter não pagina com `offset/limit`; usa Spring Data + `SortUtils`:

```java
Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
Page<ProdutoEntity> result = repository.findAll(filter, pageable);
```

`SortUtils` = `br.com.archbase.query.rsql.jpa.SortUtils`.

---

## Tabela de Operações QueryDSL

| Operação | Sintaxe | Descrição |
|----------|---------|-----------|
| `eq` / `ne` | `qProduto.sku.eq("x")` | Igual / diferente |
| `between` | `qProduto.preco.between(min, max)` | Entre |
| `isTrue` / `isFalse` | `qProduto.ativo.isTrue()` | Booleano |
| `equalsIgnoreCase` | `qProduto.marca.equalsIgnoreCase(m)` | Texto sem caixa |
| `containsIgnoreCase` | `qProduto.nome.containsIgnoreCase(n)` | Contém |
| `gt` / `goe` / `lt` / `loe` | `qProduto.estoque.goe(10)` | Comparações |
| `in` | `qProduto.categoria.in(lista)` | Em lista |
| `isNull` / `isNotNull` | `qProduto.marca.isNotNull()` | Nulo |
| `count` / `min` / `max` / `avg` | `qProduto.preco.max()` | Agregações |
| `groupBy` | `.groupBy(qProduto.categoria)` | Agrupar |
| `orderBy` | `.orderBy(qProduto.nome.asc())` | Ordenar |

---

**IMPORTANTE**: Queries com agregação/filtro específico ficam no Adapter com QueryDSL; consultas simples por campo único ficam como query methods no `ProdutoJpaRepository`.
