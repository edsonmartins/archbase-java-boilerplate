# 02. Domain Objects

Domain Objects são objetos puros do domínio, sem dependências de infraestrutura (JPA, Spring, etc.).

---

## Conceito

**CRÍTICO**: Domain Objects devem ser:
- Puros Java (sem anotações JPA)
- Independentes de frameworks
- Focados no negócio
- Com equals(), hashCode() e toString()

---

## Estrutura Básica

```java
package br.com.empresa.projeto.core.domain.entity;

import java.util.Objects;

public class Example {

    private String id;
    private String nome;
    private String descricao;
    private Boolean ativo;

    public Example() {
    }

    public Example(String id, String nome, String descricao, Boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.ativo = ativo;
    }

    // Getters
    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Boolean getAtivo() { return ativo; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    // equals e hashCode baseados em ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Example example = (Example) o;
        return Objects.equals(id, example.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Example{" +
                "id='" + id + '\'' +
                ", nome='" + nome + '\'' +
                ", ativo=" + ativo +
                '}';
    }

    // Métodos de domínio
    public void activate() {
        this.ativo = true;
    }

    public void deactivate() {
        this.ativo = false;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.ativo);
    }
}
```

---

## Domain Object com Enum

```java
package br.com.empresa.projeto.core.domain.entity;

import br.com.empresa.projeto.core.domain.enums.Status;

public class Pedido {

    private String id;
    private String clienteId;
    private BigDecimal valorTotal;
    private Status status;

    public enum Status {
        CRIADO, PENDENTE_PAGAMENTO, PAGO, ENVIADO, ENTREGUE, CANCELADO
    }

    // Getters e Setters...

    // Métodos de domínio
    public void pagar() {
        if (this.status != Status.PENDENTE_PAGAMENTO) {
            throw new IllegalStateException("Pedido não está pendente de pagamento");
        }
        this.status = Status.PAGO;
    }

    public void enviar() {
        if (this.status != Status.PAGO) {
            throw new IllegalStateException("Pedido não está pago");
        }
        this.status = Status.ENVIADO;
    }

    public void cancelar() {
        if (this.status == Status.ENVIADO || this.status == Status.ENTREGUE) {
            throw new IllegalStateException("Pedido não pode ser cancelado");
        }
        this.status = Status.CANCELADO;
    }

    public boolean podePagar() {
        return this.status == Status.PENDENTE_PAGAMENTO;
    }

    public boolean podeCancelar() {
        return this.status == Status.CRIADO
            || this.status == Status.PENDENTE_PAGAMENTO;
    }
}
```

---

## Value Object

```java
package br.com.empresa.projeto.core.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

public class Dinheiro {

    private final BigDecimal valor;
    private final String moeda;

    public Dinheiro(BigDecimal valor, String moeda) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor deve ser positivo");
        }
        this.valor = valor;
        this.moeda = Objects.requireNonNull(moeda);
    }

    public BigDecimal getValor() { return valor; }
    public String getMoeda() { return moeda; }

    public Dinheiro somar(Dinheiro outro) {
        if (!this.moeda.equals(outro.moeda)) {
            throw new IllegalArgumentException("Moedas diferentes");
        }
        return new Dinheiro(this.valor.add(outro.valor), this.moeda);
    }

    public Dinheiro multiplicar(BigDecimal fator) {
        return new Dinheiro(this.valor.multiply(fator), this.moeda);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dinheiro dinheiro = (Dinheiro) o;
        return Objects.equals(valor, dinheiro.valor) &&
               Objects.equals(moeda, dinheiro.moeda);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor, moeda);
    }
}
```

---

## Domain Exception

```java
package br.com.empresa.projeto.core.domain.exception;

public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String message) {
        super(message);
        this.code = "DOMAIN_ERROR";
    }

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

```java
// Exemplo de uso específico
public class PedidoNaoEncontradoException extends DomainException {

    public PedidoNaoEncontradoException(String id) {
        super("PEDIDO_NAO_ENCONTRADO",
              "Pedido com ID " + id + " não foi encontrado");
    }
}
```

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **Sem JPA** | Não usar anotações `@Entity`, `@Column` etc. |
| **Métodos de domínio** | Lógica de negócio no domain object |
| **Imutabilidade** | Value Objects devem ser imutáveis (`final`) |
| **equals/hashCode** | Baseados em ID para entidades |
| **Validações** | Lançar `DomainException` para regras de negócio |
| **Sem dependências externas** | Apenas Java puro |

---

**IMPORTANTE**: Domain Objects são mapeados para JPA Entities via mappers, não são as próprias entities.
