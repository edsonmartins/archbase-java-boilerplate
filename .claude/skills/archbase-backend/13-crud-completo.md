# 13. CRUD Completo - Exemplo Passo a Passo

Exemplo completo de criação de CRUD seguindo todos os padrões Archbase.

---

## Cenário

Criar CRUD de **Produto** com:
- Nome, SKU, descrição, preço, categoria, estoque, ativo
- Busca com filtros
- Multi-tenant

---

## Passo 1: Domain Object

**Arquivo**: `core/domain/entity/Produto.java`

```java
package br.com.empresa.projeto.core.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Produto {

    private String id;
    private String sku;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private String categoria;
    private Integer estoque;
    private Boolean ativo;
    private String tenantId;
    private LocalDateTime dataCriacao;

    public Produto() {
        this.ativo = true;
        this.estoque = 0;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Integer getEstoque() { return estoque; }
    public void setEstoque(Integer estoque) { this.estoque = estoque; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    // Métodos de domínio
    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public boolean isAtivo() {
        return Boolean.TRUE.equals(this.ativo);
    }

    public void adicionarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        this.estoque += quantidade;
    }

    public void removerEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        if (this.estoque < quantidade) {
            throw new IllegalArgumentException("Estoque insuficiente");
        }
        this.estoque -= quantidade;
    }

    public boolean temEstoque(int quantidade) {
        return this.estoque >= quantidade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Produto produto = (Produto) o;
        return Objects.equals(id, produto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

---

## Passo 2: DTOs

**Arquivos**: `core/application/dto/ProdutoDTO.java`, `CreateProdutoDTO.java`

```java
// ProdutoDTO.java
package br.com.empresa.projeto.core.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "DTO de Produto")
public class ProdutoDTO {

    @Schema(description = "ID do produto")
    private String id;

    @Schema(description = "SKU do produto")
    private String sku;

    @Schema(description = "Nome do produto")
    private String nome;

    @Schema(description = "Descrição do produto")
    private String descricao;

    @Schema(description = "Preço do produto")
    private BigDecimal preco;

    @Schema(description = "Categoria")
    private String categoria;

    @Schema(description = "Estoque atual")
    private Integer estoque;

    @Schema(description = "Status ativo")
    private Boolean ativo;

    @Schema(description = "Data de criação")
    private LocalDateTime dataCriacao;

    // Getters e Setters (ou @Data do Lombok)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    // ... demais getters/setters
}
```

```java
// CreateProdutoDTO.java
package br.com.empresa.projeto.core.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "DTO para criação de Produto")
public class CreateProdutoDTO {

    @NotBlank(message = "SKU é obrigatório")
    @Size(min = 3, max = 50, message = "SKU deve ter entre 3 e 50 caracteres")
    @Schema(example = "PROD-001", required = true)
    private String sku;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Schema(example = "Produto Teste", required = true)
    private String nome;

    @Size(max = 500, message = "Descrição máxima 500 caracteres")
    @Schema(example = "Descrição detalhada do produto")
    private String descricao;

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser positivo")
    @Schema(example = "99.90", required = true)
    private BigDecimal preco;

    @NotBlank(message = "Categoria é obrigatória")
    @Schema(example = "ELETRONICOS", required = true)
    private String categoria;

    @Min(value = 0, message = "Estoque não pode ser negativo")
    @Schema(example = "10")
    private Integer estoque = 0;

    @Schema(example = "true")
    private Boolean ativo = true;

    // Getters e Setters
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    // ... demais getters/setters
}
```

---

## Passo 3: Entity JPA

**Arquivo**: `core/infrastructure/output/persistence/entity/ProdutoEntity.java`

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.entity;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "PRODUTO",
        indexes = {
            @Index(name = "IDX_PRODUTO_SKU", columnList = "SKU"),
            @Index(name = "IDX_PRODUTO_NOME", columnList = "NOME"),
            @Index(name = "IDX_PRODUTO_CATEGORIA", columnList = "CATEGORIA"),
            @Index(name = "IDX_PRODUTO_ATIVO", columnList = "ATIVO")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "UK_PRODUTO_SKU", columnNames = {"SKU", "TENANT_ID"})
        }
)
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "ID_PRODUTO")),
        @AttributeOverride(name = "code", column = @Column(name = "CD_PRODUTO"))
})
public class ProdutoEntity extends TenantPersistenceEntityBase {

    @Column(name = "SKU", nullable = false, length = 50)
    private String sku;

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "DESCRICAO", length = 500)
    private String descricao;

    @Column(name = "PRECO", precision = 19, scale = 2, nullable = false)
    private BigDecimal preco;

    @Column(name = "CATEGORIA", nullable = false, length = 50)
    private String categoria;

    @Column(name = "ESTOQUE")
    private Integer estoque = 0;

    @Column(name = "ATIVO")
    private Boolean ativo = true;
}
```

---

## Passo 4: Repository

**Arquivo**: `core/infrastructure/output/persistence/repository/ProdutoJpaRepository.java`

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.ProdutoEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoJpaRepository extends ArchbaseCommonJpaRepository<ProdutoEntity, String, Long> {
    // SEM métodos customizados - queries no adapter
}
```

---

## Passo 5: Mapper

**Arquivo**: `core/infrastructure/output/persistence/mapper/ProdutoMapper.java`

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.mapper;

import br.com.empresa.projeto.core.application.dto.CreateProdutoDTO;
import br.com.empresa.projeto.core.application.dto.ProdutoDTO;
import br.com.empresa.projeto.core.domain.entity.Produto;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.ProdutoEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProdutoMapper {

    // Domain ↔ DTO
    ProdutoDTO toDTO(Produto domain);
    Produto toDomain(CreateProdutoDTO dto);
    Produto toDomain(ProdutoDTO dto);

    // Domain ↔ Entity
    ProdutoEntity toEntity(Produto domain);
    Produto toDomain(ProdutoEntity entity);

    // Entity ↔ DTO
    ProdutoDTO entityToDTO(ProdutoEntity entity);

    // Listas
    java.util.List<ProdutoDTO> toDTOList(java.util.List<Produto> domains);
    java.util.List<Produto> toDomainList(java.util.List<ProdutoEntity> entities);

    // Ignorar campos de auditoria para CREATE
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createEntityDate", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "updateEntityDate", ignore = true)
    @Mapping(target = "lastModifiedByUser", ignore = true)
    @Mapping(target = "version", ignore = true)
    ProdutoEntity toEntityForCreate(Produto domain);
}
```

---

## Passo 6: Ports

**Arquivos**: `core/application/port/out/ProdutoPersistencePort.java`, `core/application/port/in/CreateProdutoUseCase.java`

```java
// ProdutoPersistencePort.java
package br.com.empresa.projeto.core.application.port.out;

import br.com.empresa.projeto.core.domain.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProdutoPersistencePort {

    Produto save(Produto produto);

    Optional<Produto> findById(String id);

    Optional<Produto> findBySku(String sku, String tenantId);

    List<Produto> findByTenantId(String tenantId);

    List<Produto> findByAtivoAndTenantId(Boolean ativo, String tenantId);

    Page<Produto> findAll(String tenantId, Pageable pageable);

    void deleteById(String id);

    boolean existsById(String id);

    boolean existsBySku(String sku, String tenantId);
}
```

```java
// CreateProdutoUseCase.java
package br.com.empresa.projeto.core.application.port.in;

import br.com.empresa.projeto.core.application.dto.CreateProdutoDTO;
import br.com.empresa.projeto.core.application.dto.ProdutoDTO;

public interface CreateProdutoUseCase {
    ProdutoDTO create(CreateProdutoDTO dto);
}
```

---

## Passo 7: Adapter

**Arquivo**: `core/infrastructure/output/persistence/adapter/ProdutoPersistenceAdapter.java`

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.adapter;

import br.com.empresa.projeto.core.application.port.out.ProdutoPersistencePort;
import br.com.empresa.projeto.core.domain.entity.Produto;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.QProdutoEntity;
import br.com.empresa.projeto.core.infrastructure.output.persistence.mapper.ProdutoMapper;
import br.com.empresa.projeto.core.infrastructure.output.persistence.repository.ProdutoJpaRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProdutoPersistenceAdapter implements ProdutoPersistencePort {

    private final ProdutoJpaRepository repository;
    private final JPAQueryFactory queryFactory;
    private final ProdutoMapper mapper;

    @Override
    @Transactional
    public Produto save(Produto produto) {
        log.debug("Salvando produto: {}", produto.getSku());
        ProdutoEntity entity = mapper.toEntityForCreate(produto);
        ProdutoEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Produto> findById(String id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Produto> findBySku(String sku, String tenantId) {
        QProdutoEntity produto = QProdutoEntity.produtoEntity;
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(produto)
                        .where(produto.sku.eq(sku)
                                .and(produto.tenantId.eq(tenantId)))
                        .fetchOne()
        ).map(mapper::toDomain);
    }

    @Override
    public List<Produto> findByTenantId(String tenantId) {
        QProdutoEntity produto = QProdutoEntity.produtoEntity;
        return queryFactory
                .selectFrom(produto)
                .where(produto.tenantId.eq(tenantId))
                .fetch()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Produto> findByAtivoAndTenantId(Boolean ativo, String tenantId) {
        QProdutoEntity produto = QProdutoEntity.produtoEntity;
        return queryFactory
                .selectFrom(produto)
                .where(produto.tenantId.eq(tenantId)
                        .and(produto.ativo.eq(ativo)))
                .fetch()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Produto> findAll(String tenantId, Pageable pageable) {
        QProdutoEntity produto = QProdutoEntity.produtoEntity;
        long total = queryFactory
                .select(produto.count())
                .from(produto)
                .where(produto.tenantId.eq(tenantId))
                .fetchOne();

        List<Produto> content = queryFactory
                .selectFrom(produto)
                .where(produto.tenantId.eq(tenantId))
                .orderBy(produto.nome.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(mapper::toDomain)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsBySku(String sku, String tenantId) {
        QProdutoEntity produto = QProdutoEntity.produtoEntity;
        return queryFactory
                .selectFrom(produto)
                .where(produto.sku.eq(sku)
                        .and(produto.tenantId.eq(tenantId)))
                .fetchFirst() != null;
    }
}
```

---

## Passo 8: Service

**Arquivo**: `core/application/service/ProdutoService.java`

```java
package br.com.empresa.projeto.core.application.service;

import br.com.archbase.multitenancy.ArchbaseCurrentTenantIdentifierResolver;
import br.com.empresa.projeto.core.application.dto.CreateProdutoDTO;
import br.com.empresa.projeto.core.application.dto.ProdutoDTO;
import br.com.empresa.projeto.core.application.mapper.ProdutoMapper;
import br.com.empresa.projeto.core.application.port.in.CreateProdutoUseCase;
import br.com.empresa.projeto.core.application.port.out.ProdutoPersistencePort;
import br.com.empresa.projeto.core.domain.entity.Produto;
import br.com.empresa.projeto.core.domain.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService implements CreateProdutoUseCase {

    private final ProdutoPersistencePort persistencePort;
    private final ProdutoMapper mapper;
    private final ArchbaseCurrentTenantIdentifierResolver tenantResolver;

    @Override
    @Transactional
    public ProdutoDTO create(CreateProdutoDTO dto) {
        log.info("Criando produto: {}", dto.getSku());

        String tenantId = tenantResolver.getCurrentTenantId();

        // Validar SKU único
        if (persistencePort.existsBySku(dto.getSku(), tenantId)) {
            throw new DomainException("Já existe produto com este SKU");
        }

        // Criar domain object
        Produto produto = new Produto();
        produto.setId(UUID.randomUUID().toString());
        produto.setSku(dto.getSku());
        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        produto.setPreco(dto.getPreco());
        produto.setCategoria(dto.getCategoria());
        produto.setEstoque(dto.getEstoque() != null ? dto.getEstoque() : 0);
        produto.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        produto.setTenantId(tenantId);

        // Salvar
        Produto saved = persistencePort.save(produto);

        log.info("Produto criado: {}", saved.getId());
        return mapper.toDTO(saved);
    }
}
```

---

## Passo 9: Controller

**Arquivo**: `rest/infrastructure/input/rest/ProdutoController.java`

```java
package br.com.empresa.projeto.rest.infrastructure.input.rest;

import br.com.empresa.projeto.core.application.dto.CreateProdutoDTO;
import br.com.empresa.projeto.core.application.dto.ProdutoDTO;
import br.com.empresa.projeto.core.application.port.in.CreateProdutoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos")
public class ProdutoController {

    private final CreateProdutoUseCase createUseCase;

    @PostMapping
    @Operation(summary = "Criar novo produto")
    public ResponseEntity<ProdutoDTO> create(
            @Valid @RequestBody CreateProdutoDTO dto) {

        ProdutoDTO created = createUseCase.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

---

## Resumo dos Arquivos Criados

| # | Arquivo | Caminho |
|---|---------|---------|
| 1 | `Produto.java` | `core/domain/entity/` |
| 2 | `ProdutoDTO.java` | `core/application/dto/` |
| 3 | `CreateProdutoDTO.java` | `core/application/dto/` |
| 4 | `ProdutoEntity.java` | `core/infrastructure/output/persistence/entity/` |
| 5 | `ProdutoJpaRepository.java` | `core/infrastructure/output/persistence/repository/` |
| 6 | `ProdutoMapper.java` | `core/infrastructure/output/persistence/mapper/` |
| 7 | `ProdutoPersistencePort.java` | `core/application/port/out/` |
| 8 | `CreateProdutoUseCase.java` | `core/application/port/in/` |
| 9 | `ProdutoPersistenceAdapter.java` | `core/infrastructure/output/persistence/adapter/` |
| 10 | `ProdutoService.java` | `core/application/service/` |
| 11 | `ProdutoController.java` | `rest/infrastructure/input/rest/` |

---

**CRÍTICO**: Esta é a estrutura completa padrão. Siga estes passos para qualquer CRUD.
