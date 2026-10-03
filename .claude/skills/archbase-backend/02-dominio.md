# 02. Domain Objects

Domain Objects são objetos de domínio sem dependências de infraestrutura (JPA, Spring etc.). O exemplo real é
`Produto`.

---

## Conceito

O `Produto` do boilerplate:
- Fica em `br.com.archbase.boilerplate.core.domain.entity`
- Não tem anotações JPA nem Spring (só Lombok e tipos Java)
- Usa Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` (equals/hashCode/toString gerados
  pelo `@Data`, sobre todos os campos)
- Tem método de fábrica estático e métodos de domínio com a regra de negócio

---

## Estrutura Real: Produto

```java
package br.com.archbase.boilerplate.core.domain.entity;

import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Produto {

    private String id;
    private String nome;
    private String sku;
    private String descricao;
    private BigDecimal preco;
    private CategoriaProduto categoria;
    private Integer estoque;
    private Boolean ativo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private String tenantId;
    private Boolean destaque;
    private String urlImagem;
    private String marca;

    /** Método de fábrica para criar um novo produto. */
    public static Produto create(String nome, String sku, BigDecimal preco, CategoriaProduto categoria) {
        return Produto.builder()
                .id(UUID.randomUUID().toString())
                .nome(nome)
                .sku(sku)
                .preco(preco)
                .categoria(categoria)
                .ativo(true)
                .destaque(false)
                .estoque(0)
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacao(LocalDateTime.now())
                .build();
    }

    public void ativar() {
        this.ativo = true;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void desativar() {
        this.ativo = false;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void adicionarEstoque(Integer quantidade) {
        this.estoque += quantidade;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void removerEstoque(Integer quantidade) {
        if (this.estoque < quantidade) {
            throw new IllegalArgumentException("Estoque insuficiente");
        }
        this.estoque -= quantidade;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void atualizarPreco(BigDecimal novoPreco) {
        if (novoPreco == null || novoPreco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço inválido");
        }
        this.preco = novoPreco;
        this.dataAtualizacao = LocalDateTime.now();
    }
}
```

---

## Enum de Domínio

Enums ficam em `core.domain.enums` (não aninhados no domain object):

```java
package br.com.archbase.boilerplate.core.domain.enums;

import lombok.Getter;

@Getter
public enum CategoriaProduto {

    ELETRONICOS("Eletrônicos"),
    MOVEIS("Móveis"),
    ROUPAS("Roupas"),
    VESTUARIO("Vestuário"),
    ALIMENTOS("Alimentos"),
    BEBIDAS("Bebidas"),
    LIMPEZA("Limpeza"),
    HIGIENE("Higiene"),
    ESPORTES("Esportes"),
    LIVROS("Livros"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaProduto(String descricao) {
        this.descricao = descricao;
    }
}
```

Na entidade JPA o enum é gravado com `@Enumerated(EnumType.STRING)`; a migration V1 tem um CHECK com esses nomes —
ao acrescentar um valor ao enum, acrescente também uma migration que altere o CHECK.

---

## Domain Exceptions

Hierarquia real em `br.com.archbase.boilerplate.core.domain.exception`, todas `RuntimeException`:

```java
public class BoilerplateException extends RuntimeException {      // base
    public BoilerplateException(String message) { super(message); }
    public BoilerplateException(String message, Throwable cause) { super(message, cause); }
}

// extends BoilerplateException
public class EntityNotFoundException      // (entityName, identifier) ou (entityName, fieldName, identifier)
public class DuplicateEntityException     // (entityName, field, value) -> "%s já existe com %s: %s"
public class BusinessValidationException  // (message) ou (field, message); getField()
```

Uso no `ProdutoService`:

```java
if (dto.getSku() != null && repository.existsBySku(dto.getSku())) {
    throw new DuplicateEntityException("Produto", "SKU", dto.getSku());
}
```

O `RestExceptionHandler` (módulo rest) tem handler para `EntityNotFoundException`, `DuplicateEntityException`,
`BusinessValidationException` e a base `BoilerplateException`, além de `IllegalArgumentException` (que é o que
`Produto.removerEstoque`/`atualizarPreco` lançam) e outras exceções de Spring/JPA. Exceção de negócio nova deve
estender `BoilerplateException`.

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **Sem JPA** | Não usar `@Entity`, `@Column` etc. no domain object |
| **Métodos de domínio** | Regra de negócio no domain object (`ativar`, `removerEstoque`...) |
| **Fábrica estática** | `Produto.create(...)` define os defaults (ativo, estoque 0, datas) |
| **Lombok** | `@Data` + `@Builder` + construtores, como em `Produto` |
| **Exceções** | Estender `BoilerplateException`; o `RestExceptionHandler` converte em `ApiError` |
| **Sem frameworks** | Apenas Java e Lombok |

---

**IMPORTANTE**: Domain Objects são mapeados para JPA Entities via mapper (`ProdutoPersistenceMapper`), não são as
próprias entities. Hoje o fluxo HTTP do `Produto` não passa pelo domain object: `ProdutoService` trabalha com
`ProdutoEntity` e DTOs. O domain object só é usado pelo `ProdutoPersistenceAdapter`.
