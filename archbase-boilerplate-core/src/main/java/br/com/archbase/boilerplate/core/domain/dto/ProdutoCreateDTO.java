package br.com.archbase.boilerplate.core.domain.dto;

import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para criação de novos produtos.
 * Contém apenas os campos necessários para criar um produto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoCreateDTO {

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 200, message = "O nome deve ter entre 2 e 200 caracteres")
    private String nome;

    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
    private String descricao;

    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal preco;

    @PositiveOrZero(message = "O estoque não pode ser negativo")
    private Integer estoque;

    @NotNull(message = "A categoria é obrigatória")
    private CategoriaProduto categoria;

    @Size(max = 100, message = "O SKU deve ter no máximo 100 caracteres")
    private String sku;

    @Size(max = 100, message = "A marca deve ter no máximo 100 caracteres")
    private String marca;

    @Size(max = 500, message = "A URL da imagem deve ter no máximo 500 caracteres")
    private String urlImagem;

    private Boolean destaque;
}
