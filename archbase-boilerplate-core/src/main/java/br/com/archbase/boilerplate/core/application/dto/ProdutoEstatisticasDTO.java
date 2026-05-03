package br.com.archbase.boilerplate.core.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO com estatísticas agregadas de produtos.
 */
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
