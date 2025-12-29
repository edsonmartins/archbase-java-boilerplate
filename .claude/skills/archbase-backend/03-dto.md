# 03. DTOs e Validações

Data Transfer Objects para entrada e saída de dados da API.

---

## Conceito

**CRÍTICO**: DTOs servem para:
- Transferir dados entre camadas
- Validar entrada do usuário
- Ocultar entidades internas
- Controlar exposição de dados

---

## DTO de Resposta

```java
package br.com.empresa.projeto.core.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "DTO de resposta de Example")
public class ExampleDTO {

    @Schema(description = "ID do exemplo")
    private String id;

    @Schema(description = "Nome do exemplo")
    private String nome;

    @Schema(description = "Descrição do exemplo")
    private String descricao;

    @Schema(description = "Status ativo")
    private Boolean ativo;

    @Schema(description = "Data de criação")
    private LocalDateTime dataCriacao;

    @Schema(description = "ID do tenant")
    private String tenantId;

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
```

---

## DTO de Criação

```java
package br.com.empresa.projeto.core.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para criação de Example")
public class CreateExampleDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome do exemplo", example = "Exemplo Teste", required = true)
    private String nome;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    @Schema(description = "Descrição do exemplo", example = "Descrição detalhada")
    private String descricao;

    @Schema(description = "Status ativo", example = "true")
    private Boolean ativo = true;

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
```

---

## DTO de Atualização

```java
package br.com.empresa.projeto.core.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Optional;

@Schema(description = "DTO para atualização de Example")
public class UpdateExampleDTO {

    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome do exemplo")
    private String nome;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    @Schema(description = "Descrição do exemplo")
    private String descricao;

    @Schema(description = "Status ativo")
    private Boolean ativo;

    // Getters e Setters
    public Optional<String> getNome() { return Optional.ofNullable(nome); }
    public void setNome(String nome) { this.nome = nome; }

    public Optional<String> getDescricao() { return Optional.ofNullable(descricao); }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Optional<Boolean> getAtivo() { return Optional.ofNullable(ativo); }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
```

---

## DTO Paginado

```java
package br.com.empresa.projeto.core.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "DTO de resposta paginada")
public class PageResponseDTO<T> {

    @Schema(description = "Conteúdo da página")
    private List<T> content;

    @Schema(description = "Número da página atual")
    private int pageNumber;

    @Schema(description = "Tamanho da página")
    private int pageSize;

    @Schema(description = "Total de elementos")
    private long totalElements;

    @Schema(description = "Total de páginas")
    private int totalPages;

    @Schema(description = "Se é a primeira página")
    private boolean first;

    @Schema(description = "Se é a última página")
    private boolean last;

    @Schema(description = "Se está vazio")
    private boolean empty;

    public static <T> PageResponseDTO<T> from(Page<T> page) {
        PageResponseDTO<T> response = new PageResponseDTO<>();
        response.content = page.getContent();
        response.pageNumber = page.getNumber();
        response.pageSize = page.getSize();
        response.totalElements = page.getTotalElements();
        response.totalPages = page.getTotalPages();
        response.first = page.isFirst();
        response.last = page.isLast();
        response.empty = page.isEmpty();
        return response;
    }

    // Getters e Setters...
}
```

---

## Validações Bean Validation

```java
package br.com.empresa.projeto.core.application.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CreatePedidoDTO {

    @NotNull(message = "Cliente ID é obrigatório")
    private String clienteId;

    @NotEmpty(message = "Itens é obrigatório")
    @Size(min = 1, message = "Pedido deve ter pelo menos 1 item")
    private List<ItemPedidoDTO> itens;

    @Pattern(regexp = "^[A-Z]{3}\\d{11}$", message = "CNPJ inválido")
    private String cnpj;

    @Email(message = "E-mail inválido")
    @NotBlank(message = "E-mail é obrigatório")
    private String email;

    @Min(value = 1, message = "Valor mínimo é 1")
    @Max(value = 10000, message = "Valor máximo é 10000")
    private Integer quantidade;

    @Positive(message = "Valor deve ser positivo")
    private BigDecimal valor;

    @Future(message = "Data deve ser futura")
    private LocalDate dataEntrega;

    @PastOrPresent(message = "Data não pode ser futura")
    private LocalDateTime dataCriacao;

    @AssertTrue(message = "Termos devem ser aceitos")
    private Boolean termosAceitos;

    // Getters e Setters...
}
```

---

## Validação Customizada

```java
// Annotation customizada
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CnpjValidator.class)
public @interface Cnpj {
    String message() default "CNPJ inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator
public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Use @NotNull para required
        }
        return validarCNPJ(value);
    }

    private boolean validarCNPJ(String cnpj) {
        // Lógica de validação de CNPJ
        cnpj = cnpj.replaceAll("[^0-9]", "");
        if (cnpj.length() != 14) return false;
        // ... validação completa
        return true;
    }
}

// Uso
public class CreateFornecedorDTO {
    @Cnpj(message = "CNPJ inválido")
    private String cnpj;
}
```

---

## Lista de Validações Comuns

| Annotation | Descrição | Exemplo |
|------------|-----------|---------|
| `@NotNull` | Campo obrigatório | `@NotNull private String nome;` |
| `@NotBlank` | String não vazia | `@NotBlank private String descricao;` |
| `@NotEmpty` | Coleção não vazia | `@NotEmpty private List<String> itens;` |
| `@Size` | Tamanho mínimo/máximo | `@Size(min=3, max=100)` |
| `@Min` / `@Max` | Valor numérico | `@Min(1) private Integer qtd;` |
| `@Positive` | Número positivo | `@Positive private BigDecimal valor;` |
| `@Email` | E-mail válido | `@Email private String email;` |
| `@Pattern` | Expressão regular | `@Pattern(regexp="^[A-Z]{2}\\d{9}$")` |
| `@Past` / `@Future` | Data passada/futura | `@Future private LocalDate data;` |
| `@AssertTrue` | Deve ser true | `@AssertTrue private Boolean aceito;` |

---

## Response Padrão

```java
package br.com.empresa.projeto.core.application.dto;

@Schema(description = "Response padrão da API")
public class ResponseDTO<T> {

    @Schema(description = "Dados de resposta")
    private T data;

    @Schema(description = "Mensagem")
    private String message;

    public static <T> ResponseDTO<T> ok(T data) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.data = data;
        return response;
    }

    public static <T> ResponseDTO<T> ok(T data, String message) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.data = data;
        response.message = message;
        return response;
    }

    // Getters e Setters...
}
```

---

**IMPORTANTE**: Sempre use `@Valid` no controller para validar DTOs automaticamente.
