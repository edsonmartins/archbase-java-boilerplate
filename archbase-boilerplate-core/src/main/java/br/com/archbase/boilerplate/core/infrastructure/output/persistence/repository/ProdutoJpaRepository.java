package br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository;

import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.ProdutoEntity;
import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA para Produto.
 */
@Repository
public interface ProdutoJpaRepository extends ArchbaseCommonJpaRepository<ProdutoEntity, String, Long> {

    /**
     * Busca produto por SKU.
     */
    Optional<ProdutoEntity> findBySku(String sku);

    /**
     * Verifica se SKU já existe.
     */
    boolean existsBySku(String sku);

    /**
     * Busca produtos por categoria.
     */
    java.util.List<ProdutoEntity> findByCategoria(br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto categoria);

    /**
     * Busca produtos ativos.
     */
    java.util.List<ProdutoEntity> findByAtivoTrue();
}
