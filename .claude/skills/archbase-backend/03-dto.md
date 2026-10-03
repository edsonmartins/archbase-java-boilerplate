# 03. DTOs e Validações

Data Transfer Objects para entrada e saída de dados da API. Exemplos reais: `ProdutoDTO`, `ProdutoCreateDTO`,
`ProdutoUpdateDTO`, `ProdutoEstatisticasDTO`, em `br.com.archbase.boilerplate.core.application.dto`.

---

## Conceito

DTOs servem para:
- Transferir dados entre camadas
- Validar entrada do usuário (Bean Validation `jakarta.validation`)
- Não expor a entidade JPA na API
- Controlar o que entra e o que sai

Padrão Lombok dos DTOs do boilerplate: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`.
Os DTOs atuais não usam `@Schema` do OpenAPI (a documentação fica em `@Operation`/`@Parameter` no controller).

---

## DTO de Resposta (ProdutoDTO)

Inclui os campos de auditoria/tenant herdados da base da entidade e os campos de negócio:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDTO {

    private String id;
    private String code;
    private Long version;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createEntityDate;

    private String createdByUser;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateEntityDate;

    private String lastModifiedByUser;
    private String tenantId;

    // Campos de negócio
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private Integer estoque;
    private CategoriaProduto categoria;
    private Boolean ativo;
    private String sku;
    private String marca;
    private String urlImagem;
    private Boolean destaque;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCadastro;
}
```

---

## DTO de Criação (ProdutoCreateDTO)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoCreateDTO {

    @NotBlank(message = "O nome e obrigatorio")
    @Size(min = 2, max = 200, message = "O nome deve ter entre 2 e 200 caracteres")
    private String nome;

    @Size(max = 1000, message = "A descricao deve ter no maximo 1000 caracteres")
    private String descricao;

    @NotNull(message = "O preco e obrigatorio")
    @Positive(message = "O preco deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "O preco deve ter no maximo 8 digitos inteiros e 2 decimais")
    private BigDecimal preco;

    @PositiveOrZero(message = "O estoque nao pode ser negativo")
    private Integer estoque;

    @NotNull(message = "A categoria e obrigatoria")
    private CategoriaProduto categoria;

    @Size(max = 100, message = "O SKU deve ter no maximo 100 caracteres")
    private String sku;

    @Size(max = 100, message = "A marca deve ter no maximo 100 caracteres")
    private String marca;

    @Size(max = 500, message = "A URL da imagem deve ter no maximo 500 caracteres")
    private String urlImagem;

    private Boolean destaque;
}
```

Os limites de `@Size` espelham o `length` das colunas em `ProdutoEntity` (nome 200, descrição 1000, sku 100,
marca 100, url_imagem 500); `@Digits(integer = 8, fraction = 2)` cabe no `numeric(10,2)` do preço. Mantenha os dois
lados alinhados. As mensagens do boilerplate estão sem acento.

---

## DTO de Atualização (ProdutoUpdateDTO)

Atualização parcial: todos os campos são opcionais (sem `@NotNull`/`@NotBlank`); a validação só vale quando o campo
vem preenchido. Campos nulos são ignorados pelo service. Os getters são os do Lombok, não `Optional`.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoUpdateDTO {

    @Size(min = 2, max = 200, message = "O nome deve ter entre 2 e 200 caracteres")
    private String nome;

    @Size(max = 1000, message = "A descricao deve ter no maximo 1000 caracteres")
    private String descricao;

    @Positive(message = "O preco deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "O preco deve ter no maximo 8 digitos inteiros e 2 decimais")
    private BigDecimal preco;

    @PositiveOrZero(message = "O estoque nao pode ser negativo")
    private Integer estoque;

    private CategoriaProduto categoria;
    private Boolean ativo;

    @Size(max = 100, message = "O SKU deve ter no maximo 100 caracteres")
    private String sku;

    @Size(max = 100, message = "A marca deve ter no maximo 100 caracteres")
    private String marca;

    @Size(max = 500, message = "A URL da imagem deve ter no maximo 500 caracteres")
    private String urlImagem;

    private Boolean destaque;
}
```

---

## DTO de Estatísticas (ProdutoEstatisticasDTO)

DTO de leitura com agregados, só campos e Lombok:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoEstatisticasDTO {

    private Long totalProdutos;
    private Long produtosAtivos;
    private Long produtosInativos;
    private Long produtosEmDestaque;
    private BigDecimal precoMedio;
    private BigDecimal precoMinimo;
    private BigDecimal precoMaximo;
    private Long estoqueTotal;
    private Map<String, Long> produtosPorCategoria;
    private Map<String, Long> produtosPorMarca;
}
```

---

## Paginação

Não há DTO de página próprio. Os endpoints `GET /api/v1/produtos/findAll?page=&size=[&sort=]` retornam
`org.springframework.data.domain.Page<ProdutoDTO>` diretamente (`ProdutoService.buscarTodos(page, size[, sort])`).

---

## Uso no Controller

```java
@PostMapping
public ResponseEntity<ProdutoDTO> criar(@Valid @RequestBody ProdutoCreateDTO dto) { ... }

@PutMapping("/{id}")
public ResponseEntity<ProdutoDTO> atualizar(@PathVariable String id,
                                            @Valid @RequestBody ProdutoUpdateDTO dto) { ... }
```

O controller tem `@Validated` na classe e `@Valid` nos `@RequestBody`. O `ProdutoService` converte
`ProdutoEntity` em DTO via `entity.toDTO()` (e `ProdutoEntity.fromDTO(dto)` para o caminho `ProdutoDTO`, mantido
por compatibilidade). Existe ainda `ProdutoPersistenceMapper` (MapStruct) com `toDTO`/`toDomain`/`entityToDTO`,
usado pelo adapter.

---

## Anotações Bean Validation comuns

| Annotation | Descrição | Usada em Produto |
|------------|-----------|------------------|
| `@NotNull` | Campo obrigatório | `preco`, `categoria` (Create) |
| `@NotBlank` | String não vazia | `nome` (Create) |
| `@Size` | Tamanho mínimo/máximo | `nome`, `descricao`, `sku`, `marca`, `urlImagem` |
| `@Positive` | Número maior que zero | `preco` |
| `@PositiveOrZero` | Zero ou positivo | `estoque` |
| `@Digits` | Dígitos inteiros/decimais | `preco` |
| `@NotEmpty`, `@Min`/`@Max`, `@Email`, `@Pattern`, `@Past`/`@Future`, `@AssertTrue` | Disponíveis em `jakarta.validation.constraints` | não usadas hoje |

Para validação customizada, crie a annotation com `@Constraint(validatedBy = ...)` e o
`ConstraintValidator` (padrão Jakarta Validation; não há exemplo no repositório).

---

**IMPORTANTE**: Sempre use `@Valid` no `@RequestBody` do controller para validar DTOs automaticamente.
