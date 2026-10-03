# 04. Entity Pattern

Entidades JPA que persistem os dados no banco. Exemplo real: `ProdutoEntity`, em
`br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity`.

---

## Conceito

A entidade do boilerplate:
- Estende `TenantPersistenceEntityBase` (`br.com.archbase.ddd.domain.base`, jar `archbase-domain-driven-design-spec`)
- Usa Lombok `@Getter` e `@Setter` na classe; `@Builder` fica **no construtor completo** (não na classe)
- Declara o construtor sem argumentos (defaults) e um construtor com `@Builder` que repassa os campos da base
- Tem apenas mapeamento JPA e conversão simples (`toDTO()` / `fromDTO()`); regra de negócio fica no service/domínio

O que a base fornece (colunas reais, conferidas no código-fonte e no `V1__schema_inicial.sql`):

| Campo Java | Coluna | Observação |
|------------|--------|------------|
| `id` (String) | `ID` (40) | `@Id`; UUID gerado no construtor sem argumentos |
| `code` (String) | `CODIGO` (40) | |
| `version` (Long) | `VERSAO` | `@Version` (optimistic locking) |
| `createEntityDate` | `DH_CRIACAO` | preenchida por construtor, não por `@CreatedDate` |
| `createdByUser` | `USUARIO_CRIOU` | `@CreatedBy`, via `AuditingEntityListener` |
| `updateEntityDate` | `DH_ATUALIZACAO` | quem altera deve preencher explicitamente |
| `lastModifiedByUser` | `ULTIMO_USUARIO_ALTEROU` | `@LastModifiedBy` |
| `tenantId` (String) | `TENANT_ID` (40) | `@TenantId` do Hibernate: filtro automático por tenant |

Por isso o `ProdutoEntity` **não** usa `@AttributeOverride`: as colunas da base ficam com os nomes acima e a
migration (`produto`) as contém (`id`, `codigo`, `versao`, `dh_criacao`, `usuario_criou`, `dh_atualizacao`,
`ultimo_usuario_alterou`, `tenant_id`).

---

## Entity Real: ProdutoEntity

```java
package br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity;

import br.com.archbase.boilerplate.core.application.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "produto", indexes = {
        @Index(name = "idx_produto_nome", columnList = "nome"),
        @Index(name = "idx_produto_sku", columnList = "sku", unique = true),
        @Index(name = "idx_produto_categoria", columnList = "categoria"),
        @Index(name = "idx_produto_ativo", columnList = "ativo")
})
@Getter
@Setter
public class ProdutoEntity extends TenantPersistenceEntityBase {

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "descricao", length = 1000)
    private String descricao;

    @Column(name = "preco", precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "estoque")
    private Integer estoque;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 30)
    private CategoriaProduto categoria;

    @Column(name = "ativo")
    private Boolean ativo;

    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;

    @Column(name = "destaque")
    private Boolean destaque;

    @Column(name = "url_imagem", length = 500)
    private String urlImagem;

    @Column(name = "marca", length = 100)
    private String marca;

    public ProdutoEntity() {
        super();
        this.ativo = true;
        this.estoque = 0;
        this.destaque = false;
    }

    @Builder
    public ProdutoEntity(String id, String code, Long version, LocalDateTime createEntityDate,
                         String createdByUser, LocalDateTime updateEntityDate,
                         String lastModifiedByUser, String tenantId,
                         String nome, String descricao, BigDecimal preco, Integer estoque,
                         CategoriaProduto categoria, Boolean ativo, String sku, LocalDateTime dataCadastro,
                         Boolean destaque, String urlImagem, String marca) {
        // id e createEntityDate são gerados aqui quando não vêm informados: a sobrecarga do
        // construtor da base que recebe argumentos NÃO gera UUID nem data (só o construtor sem
        // argumentos faz isso). Sem isto, o POST falhava com "Identifier of entity ... must be
        // manually assigned before calling 'persist()'".
        super(id != null ? id : java.util.UUID.randomUUID().toString(),
                code, version,
                createEntityDate != null ? createEntityDate : LocalDateTime.now(),
                createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque != null ? estoque : 0;
        this.categoria = categoria;
        this.ativo = ativo != null ? ativo : true;
        this.sku = sku;
        this.dataCadastro = dataCadastro;
        this.destaque = destaque != null ? destaque : false;
        this.urlImagem = urlImagem;
        this.marca = marca;
    }

    public static ProdutoEntity fromDTO(ProdutoDTO dto) { ... }   // via builder

    public ProdutoDTO toDTO() { ... }                              // via ProdutoDTO.builder()
}
```

Detalhes a conhecer:
- Nomes de tabela e coluna em minúsculas (`produto`, `url_imagem`), não em MAIÚSCULAS como as colunas da base.
- `@Builder` no construtor com argumentos da base é o que permite `ProdutoEntity.builder()...build()` (usado no
  `ProdutoService.criar`). Ao criar nova entidade a partir deste exemplo, mantenha a geração de `id` e
  `createEntityDate` nesse construtor.
- `fromDTO()` e `toDTO()` copiam todos os campos, inclusive `destaque`, `urlImagem` e `marca` (coberto por
  `ProdutoEntityTest`); `toDTO()` usa `getTenantId()`. Ao adicionar campo, atualize os dois métodos.
- `updateEntityDate` não é preenchido automaticamente: o `ProdutoService.atualizar` faz
  `entity.setUpdateEntityDate(LocalDateTime.now())`.

---

## Repository

```java
@Repository
public interface ProdutoJpaRepository extends ArchbaseCommonJpaRepository<ProdutoEntity, String, Long> {

    Optional<ProdutoEntity> findBySku(String sku);
    boolean existsBySku(String sku);
    List<ProdutoEntity> findByCategoria(CategoriaProduto categoria);
    List<ProdutoEntity> findByAtivoTrue();
}
```

`ArchbaseCommonJpaRepository` (`br.com.archbase.ddd.infraestructure.persistence.jpa.repository`) estende
`JpaSpecificationExecutor` e acrescenta `query(...)`, `update(...)`, `deleteWhere(Predicate)`, `findOne`/`findAll`
com QueryDSL (`JPQLQuery`, `FactoryExpression`, `Predicate`). A implementação base é
`CommonArchbaseJpaRepository`, configurada em `@EnableJpaRepositories(repositoryBaseClass = ...)`.

---

## Classes Q (QueryDSL)

O annotation processor `io.github.openfeign.querydsl:querydsl-apt` (classifier `jakarta`) gera
`QProdutoEntity` em `target/generated-sources/annotations`, no mesmo pacote da entidade. O
`ProdutoPersistenceAdapter` a usa assim:

```java
private static final QProdutoEntity qProduto = QProdutoEntity.produtoEntity;
private final JPAQueryFactory queryFactory;   // bean fornecido por QueryDslConfig
```

---

## Tabela e Migration

Como o schema de produção é validado (`ddl-auto: validate`), toda entidade nova precisa de migration Flyway em
`archbase-boilerplate-rest/src/main/resources/db/migration`. A tabela `produto` do `V1__schema_inicial.sql`
traz também um `CHECK` para os valores do enum `categoria`.

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **Base multi-tenant** | Estender `TenantPersistenceEntityBase` |
| **@Getter/@Setter** | Lombok na classe; `@Builder` apenas no construtor com argumentos, como em `ProdutoEntity` |
| **Gerar id/data no construtor do builder** | A base só gera UUID e data no construtor sem argumentos |
| **Enum** | `@Enumerated(EnumType.STRING)` com `length` definido |
| **Indexes e unique** | Declarar em `@Table(indexes = ...)`; sku é `unique` |
| **Tamanho de colunas** | Sempre definir `length` para String; alinhar com `@Size` dos DTOs |
| **Relacionamentos** | `FetchType.LAZY` em `@ManyToOne`/`@OneToMany` (o Produto não tem relacionamentos; não há exemplo no repositório) |
| **Migration** | Escrever a migration Flyway ao criar/alterar a entidade |

---

**IMPORTANTE**: Não coloque regra de negócio na Entity. A regra fica no domain object (`Produto`) ou no service.
