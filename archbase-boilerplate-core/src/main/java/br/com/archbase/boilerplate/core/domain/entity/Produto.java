package br.com.archbase.boilerplate.core.domain.entity;

import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de domínio para Produto.
 * <p>
 * Objeto puro de domínio, sem dependências de frameworks de persistência.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Produto {

    /**
     * Identificador único do produto.
     */
    private String id;

    /**
     * Nome do produto.
     */
    private String nome;

    /**
     * SKU do produto.
     */
    private String sku;

    /**
     * Descrição do produto.
     */
    private String descricao;

    /**
     * Preço do produto.
     */
    private BigDecimal preco;

    /**
     * Categoria do produto.
     */
    private CategoriaProduto categoria;

    /**
     * Quantidade em estoque.
     */
    private Integer estoque;

    /**
     * Indica se o produto está ativo.
     */
    private Boolean ativo;

    /**
     * Data de criação.
     */
    private LocalDateTime dataCriacao;

    /**
     * Data de atualização.
     */
    private LocalDateTime dataAtualizacao;

    /**
     * Tenant ID para multi-tenancy.
     */
    private String tenantId;

    /**
     * Indica se o produto está em destaque.
     */
    private Boolean destaque;

    /**
     * URL da imagem do produto.
     */
    private String urlImagem;

    /**
     * Marca do produto.
     */
    private String marca;

    /**
     * Método de fábrica para criar um novo produto.
     */
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

    /**
     * Ativa o produto.
     */
    public void ativar() {
        this.ativo = true;
        this.dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Desativa o produto.
     */
    public void desativar() {
        this.ativo = false;
        this.dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Adiciona estoque.
     */
    public void adicionarEstoque(Integer quantidade) {
        this.estoque += quantidade;
        this.dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Remove estoque.
     */
    public void removerEstoque(Integer quantidade) {
        if (this.estoque < quantidade) {
            throw new IllegalArgumentException("Estoque insuficiente");
        }
        this.estoque -= quantidade;
        this.dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Atualiza preço.
     */
    public void atualizarPreco(BigDecimal novoPreco) {
        if (novoPreco == null || novoPreco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço inválido");
        }
        this.preco = novoPreco;
        this.dataAtualizacao = LocalDateTime.now();
    }
}
