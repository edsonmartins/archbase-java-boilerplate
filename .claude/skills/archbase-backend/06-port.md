# 06. Ports (Arquitetura Hexagonal)

Ports definem contratos entre a aplicação e a infraestrutura.

---

## Conceito

No boilerplate existe **apenas port de saída** (`application/port/out`). **Não há camada
`port/in` (use cases)**: o `ProdutoController` chama o `ProdutoService` diretamente.

---

## Estrutura de Pacotes (real)

```
application/port/
└── out/
    └── ProdutoPersistencePort.java
```

---

## Port Real: ProdutoPersistencePort

```java
package br.com.archbase.boilerplate.core.application.port.out;

import br.com.archbase.boilerplate.core.application.dto.ProdutoEstatisticasDTO;
import br.com.archbase.boilerplate.core.domain.entity.Produto;
import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProdutoPersistencePort extends FindDataWithFilterQuery<String, Produto> {

    Produto save(Produto produto);

    Optional<Produto> findBySku(String sku);

    List<Produto> findAll();

    List<Produto> findByCategoria(String categoria);

    List<Produto> findAtivos();

    boolean existsBySku(String sku);

    void deleteById(String id);

    ProdutoEstatisticasDTO obterEstatisticas();

    List<Produto> findByPrecoEntre(BigDecimal min, BigDecimal max);

    List<Produto> findEmDestaque();

    List<Produto> findByMarca(String marca);
}
```

---

## FindDataWithFilterQuery (RSQL)

`br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery<ID, R>` (archbase-domain-driven-design-spec 3.2.2)
define, e portanto o Port herda:

```java
R findById(ID id);                                              // o adapter devolve null se não achar
Page<R> findAll(int page, int size);
Page<R> findAll(int page, int size, String[] sort);
List<R> findAll(List<ID> ids);
Page<R> findWithFilter(String filter, int page, int size);      // filtro RSQL
Page<R> findWithFilter(String filter, int page, int size, String[] sort);
```

Estender essa interface habilita filtros dinâmicos RSQL compatíveis com os componentes de filtro do frontend.

---

## Convenções do Port real

- Trabalha com o domínio (`Produto`), não com `ProdutoEntity`.
- Sem `tenantId` nos parâmetros: o tenant é filtrado pelo Hibernate (`@TenantId` na entidade base).
- `findByCategoria(String)` recebe o nome do enum; o adapter converte com `CategoriaProduto.valueOf`.
- `obterEstatisticas()` retorna um DTO de agregação (`ProdutoEstatisticasDTO`), não domínio.

---

## Relação: Port → Adapter → Repository

```
┌──────────────────────────┐
│  ProdutoPersistencePort  │  (port/out)
└────────────┬─────────────┘
             | implementa
             ▼
┌──────────────────────────┐
│ ProdutoPersistenceAdapter│  (QueryDSL + Mapper)
└────────────┬─────────────┘
             | usa
             ▼
┌──────────────────────────┐
│   ProdutoJpaRepository   │
└──────────────────────────┘
```

**Estado atual**: nenhuma classe injeta `ProdutoPersistencePort`. O `ProdutoController` chama
`ProdutoService`, que usa `ProdutoJpaRepository` direto. O Port/Adapter existe e é o lugar das
queries QueryDSL e do RSQL, mas ainda não está ligado ao fluxo do CRUD. Se for ligar, injete o Port
no Service no lugar do repository.

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **Interfaces puras** | Port é só interface |
| **Nome** | `XxxPersistencePort` |
| **Domínio nos contratos** | Receber/retornar Domain Objects, não Entities |
| **RSQL** | Estender `FindDataWithFilterQuery<String, Dominio>` |
| **Só port/out** | Não criar `port/in` / UseCase interfaces: não é o padrão do boilerplate |

---

**IMPORTANTE**: Ports definem O QUE fazer, não COMO. A implementação fica no Adapter.
