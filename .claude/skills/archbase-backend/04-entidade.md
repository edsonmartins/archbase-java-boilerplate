# 04. Entity Pattern

Entidades JPA que persistem os Domain Objects no banco de dados.

---

## Conceito

**CRÍTICO**: Entity deve:
- Estender `TenantPersistenceEntityBase` para multi-tenant
- Usar `@Getter` e `@Setter` (SEM `@Builder`)
- Ter apenas configurações JPA, sem lógica de negócio
- Mapear para a tabela correta

---

## Entity Básica

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.entity;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "EXAMPLE",
        indexes = {
            @Index(name = "IDX_EXAMPLE_NOME", columnList = "NOME"),
            @Index(name = "IDX_EXAMPLE_ATIVO", columnList = "ATIVO")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "UK_EXAMPLE_NOME", columnNames = {"NOME", "TENANT_ID"})
        }
)
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "ID_EXAMPLE")),
        @AttributeOverride(name = "code", column = @Column(name = "CD_EXAMPLE"))
})
public class ExampleEntity extends TenantPersistenceEntityBase {

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "DESCRICAO", length = 500)
    private String descricao;

    @Column(name = "ATIVO")
    private Boolean ativo = true;
}
```

---

## Entity com Enum

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.entity;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PEDIDO")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "ID_PEDIDO"))
})
public class PedidoEntity extends TenantPersistenceEntityBase {

    @Column(name = "CLIENTE_ID", nullable = false)
    private String clienteId;

    @Column(name = "VALOR_TOTAL", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private StatusPedido status = StatusPedido.CRIADO;

    @Column(name = "DATA_PEDIDO", nullable = false)
    private LocalDateTime dataPedido;

    public enum StatusPedido {
        CRIADO, PENDENTE_PAGAMENTO, PAGO, ENVIADO, ENTREGUE, CANCELADO
    }
}
```

---

## Entity com Relacionamentos

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.entity;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CLIENTE")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "ID_CLIENTE"))
})
public class ClienteEntity extends TenantPersistenceEntityBase {

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "EMAIL", nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "TELEFONE", length = 20)
    private String telefone;

    // OneToMany - Lazy
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)  // Não gera setter para relacionamento
    private Set<PedidoEntity> pedidos = new HashSet<>();

    // Métodos helper para relacionamento
    public void adicionarPedido(PedidoEntity pedido) {
        pedidos.add(pedido);
        pedido.setCliente(this);
    }

    public void removerPedido(PedidoEntity pedido) {
        pedidos.remove(pedido);
        pedido.setCliente(null);
    }
}
```

---

## Entity com @ManyToOne

```java
@Entity
@Table(name = "PEDIDO_ITEM")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "ID_ITEM"))
})
public class PedidoItemEntity extends TenantPersistenceEntityBase {

    @Column(name = "PRODUTO_ID", nullable = false)
    private String produtoId;

    @Column(name = "PRODUTO_NOME", length = 100)
    private String produtoNome;

    @Column(name = "QUANTIDADE", nullable = false)
    private Integer quantidade = 1;

    @Column(name = "VALOR_UNITARIO", precision = 19, scale = 2)
    private BigDecimal valorUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PEDIDO_ID", nullable = false)
    private PedidoEntity pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUTO_REFERENCIA_ID")
    private ProdutoEntity produtoReferencia;
}
```

---

## @AttributeOverride para Colunas de Auditoria

```java
@Entity
@Table(name = "PRODUTO")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "ID_PRODUTO")),
        @AttributeOverride(name = "code", column = @Column(name = "CD_PRODUTO")),
        @AttributeOverride(name = "tenantId", column = @Column(name = "TENANT_ID")),
        @AttributeOverride(name = "createEntityDate", column = @Column(name = "DT_CRIACAO")),
        @AttributeOverride(name = "createdByUser", column = @Column(name = "USUARIO_CRIACAO")),
        @AttributeOverride(name = "updateEntityDate", column = @Column(name = "DT_ATUALIZACAO")),
        @AttributeOverride(name = "lastModifiedByUser", column = @Column(name = "USUARIO_ATUALIZACAO")),
        @AttributeOverride(name = "version", column = @Column(name = "VERSAO"))
})
public class ProdutoEntity extends TenantPersistenceEntityBase {
    // campos...
}
```

---

## Entity com ID Manual (String)

```java
@Entity
@Table(name = "CONFIGURACAO")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "CHAVE"))
})
public class ConfiguracaoEntity extends TenantPersistenceEntityBase {

    // Sobrescreve o ID para ser gerado manualmente
    @PrePersist
    protected void onPrePersist() {
        if (getId() == null) {
            setId(generateId());
        }
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Column(name = "VALOR", length = 1000)
    private String valor;

    @Column(name = "DESCRICAO", length = 255)
    private String descricao;
}
```

---

## Entity Embeddable

```java
// Embeddable para valor monetário
@Embeddable
public class DinheiroEmbeddable {

    @Column(name = "VALOR", precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "MOEDA", length = 3)
    private String moeda = "BRL";

    // Getters e Setters...
}

// Uso na Entity
@Entity
@Table(name = "PEDIDO")
public class PedidoEntity extends TenantPersistenceEntityBase {

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "valor", column = @Column(name = "VALOR_TOTAL")),
        @AttributeOverride(name = "moeda", column = @Column(name = "MOEDA_TOTAL"))
    })
    private DinheiroEmbeddable valorTotal;
}
```

---

## Tabela de Colunas Comuns TenantPersistenceEntityBase

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | String | ID da entidade (override com @AttributeOverride) |
| `code` | String | Código da entidade |
| `tenantId` | String | ID do tenant (filtro automático) |
| `createEntityDate` | LocalDateTime | Data de criação |
| `createdByUser` | String | Usuário que criou |
| `updateEntityDate` | LocalDateTime | Data de atualização |
| `lastModifiedByUser` | String | Último usuário a modificar |
| `version` | Long | Versão para optimistic locking |

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **Sem @Builder** | Use construtor manual se necessário |
| **@Getter/@Setter** | Use Lombok para getters/setters |
| **@AttributeOverride** | Sempre override colunas da base |
| **Lazy em relacionamentos** | `FetchType.LAZY` para @ManyToOne e @OneToMany |
| **Cascade cuidadoso** | Use `CascadeType.ALL` apenas quando apropriado |
| **Indexes** | Adicione indexes para colunas frequentemente consultadas |
| **Tamanho de colunas** | Sempre defina `length` para String |

---

**CRÍTICO**: NUNCA coloque lógica de negócio na Entity. Use Domain Objects para lógica.
