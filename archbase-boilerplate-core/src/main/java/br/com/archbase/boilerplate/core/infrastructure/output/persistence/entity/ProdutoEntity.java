package br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity;

import br.com.archbase.boilerplate.core.domain.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade JPA para persistência de Produto.
 */
@Entity
@Table(name = "produto", indexes = {
        @Index(name = "idx_produto_nome", columnList = "nome"),
        @Index(name = "idx_produto_sku", columnList = "sku", unique = true),
        @Index(name = "idx_produto_categoria", columnList = "categoria"),
        @Index(name = "idx_produto_ativo", columnList = "ativo")
})
@Getter
@Setter
public class ProdutoEntity extends TenantPersistenceEntityBase {

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "descricao", length = 1000)
    private String descricao;

    @Column(name = "preco", precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "estoque")
    private Integer estoque;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 30)
    private CategoriaProduto categoria;

    @Column(name = "ativo")
    private Boolean ativo;

    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;

    public ProdutoEntity() {
        super();
        this.ativo = true;
        this.estoque = 0;
    }

    @Builder
    public ProdutoEntity(String id, String code, Long version, LocalDateTime createEntityDate,
                         String createdByUser, LocalDateTime updateEntityDate,
                         String lastModifiedByUser, String tenantId,
                         String nome, String descricao, BigDecimal preco, Integer estoque,
                         CategoriaProduto categoria, Boolean ativo, String sku, LocalDateTime dataCadastro) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque != null ? estoque : 0;
        this.categoria = categoria;
        this.ativo = ativo != null ? ativo : true;
        this.sku = sku;
        this.dataCadastro = dataCadastro;
    }

    public static ProdutoEntity fromDTO(ProdutoDTO dto) {
        if (dto == null) return null;

        return ProdutoEntity.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .version(dto.getVersion())
                .createEntityDate(dto.getCreateEntityDate())
                .createdByUser(dto.getCreatedByUser())
                .updateEntityDate(dto.getUpdateEntityDate())
                .lastModifiedByUser(dto.getLastModifiedByUser())
                .tenantId(dto.getTenantId())
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .preco(dto.getPreco())
                .estoque(dto.getEstoque())
                .categoria(dto.getCategoria())
                .ativo(dto.getAtivo())
                .sku(dto.getSku())
                .dataCadastro(dto.getDataCadastro())
                .build();
    }

    public ProdutoDTO toDTO() {
        return ProdutoDTO.builder()
                .id(this.id)
                .code(this.code)
                .version(this.version)
                .createEntityDate(this.createEntityDate)
                .createdByUser(this.createdByUser)
                .updateEntityDate(this.updateEntityDate)
                .lastModifiedByUser(this.lastModifiedByUser)
                .tenantId(getTenantId())
                .nome(nome)
                .descricao(descricao)
                .preco(preco)
                .estoque(estoque)
                .categoria(categoria)
                .ativo(ativo)
                .sku(sku)
                .dataCadastro(dataCadastro)
                .build();
    }
}
