package br.com.archbase.boilerplate.core.infrastructure.output.persistence.mapper;

import br.com.archbase.boilerplate.core.domain.entity.Produto;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.ProdutoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Mapper para conversão entre Produto (domínio) e ProdutoEntity (persistência).
 */
@Mapper(componentModel = "spring")
public interface ProdutoPersistenceMapper {

    /**
     * Converte entidade de domínio para entidade de persistência.
     */
    ProdutoEntity toEntity(Produto produto);

    /**
     * Converte entidade de persistência para entidade de domínio.
     */
    Produto toDomain(ProdutoEntity entity);

    /**
     * Atualiza entidade de persistência com dados do domínio.
     */
    void updateEntityFromDomain(Produto produto, @MappingTarget ProdutoEntity entity);

    /**
     * Converte lista de entidades de persistência para lista de domínio.
     */
    List<Produto> toDomainList(List<ProdutoEntity> entities);
}
