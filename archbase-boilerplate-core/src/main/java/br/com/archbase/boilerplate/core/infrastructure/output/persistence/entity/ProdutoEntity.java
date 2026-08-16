package br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity;

import br.com.archbase.boilerplate.core.application.dto.ProdutoDTO;
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

    @Column(name = "destaque")
    private Boolean destaque;

    @Column(name = "url_imagem", length = 500)
    private String urlImagem;

    @Column(name = "marca", length = 100)
    private String marca;

    public ProdutoEntity() {
        super();
        this.ativo = true;
        this.estoque = 0;
        this.destaque = false;
    }

    @Builder
    public ProdutoEntity(String id, String code, Long version, LocalDateTime createEntityDate,
                         String createdByUser, LocalDateTime updateEntityDate,
                         String lastModifiedByUser, String tenantId,
                         String nome, String descricao, BigDecimal preco, Integer estoque,
                         CategoriaProduto categoria, Boolean ativo, String sku, LocalDateTime dataCadastro,
                         Boolean destaque, String urlImagem, String marca) {
        // O id é gerado aqui quando não vem informado. Quem chama o builder para criar um produto
        // novo não tem id a oferecer, e as bases do archbase só geram UUID no construtor SEM
        // argumentos — este, que repassa o id ao super, entregava null. O efeito era que criar
        // produto pelo POST falhava com "Identifier of entity ... must be manually assigned before
        // calling 'persist()'", ou seja, o CRUD de exemplo do boilerplate não funcionava.
        //
        // Gerar aqui, e não no serviço, mantém a regra num lugar só: são cinco pontos de save() no
        // ProdutoService, e o próximo CRUD copiado deste exemplo herdaria o mesmo defeito.
        // createEntityDate pelo mesmo motivo: sem isto, todo produto criado pelo POST era gravado
        // com a data de criação vazia, enquanto createdByUser vinha preenchido pelo AuditingEntityListener.
        // As datas do archbase são preenchidas por construtor, não por @CreatedDate — decisão
        // documentada na PersistenceEntityBase —, e esta sobrecarga não passa por lá.
        super(id != null ? id : java.util.UUID.randomUUID().toString(),
                code, version,
                createEntityDate != null ? createEntityDate : LocalDateTime.now(),
                createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque != null ? estoque : 0;
        this.categoria = categoria;
        this.ativo = ativo != null ? ativo : true;
        this.sku = sku;
        this.dataCadastro = dataCadastro;
        this.destaque = destaque != null ? destaque : false;
        this.urlImagem = urlImagem;
        this.marca = marca;
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
