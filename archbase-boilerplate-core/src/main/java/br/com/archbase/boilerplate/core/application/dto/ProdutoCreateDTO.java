package br.com.archbase.boilerplate.core.application.dto;

import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para criacao de novos produtos.
 * Contem apenas os campos necessarios para criar um produto.
 */
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
