package br.com.archbase.boilerplate.core.application.dto;

import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de Produto para transferencia de dados.
 */
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

    // Campos de negocio
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
