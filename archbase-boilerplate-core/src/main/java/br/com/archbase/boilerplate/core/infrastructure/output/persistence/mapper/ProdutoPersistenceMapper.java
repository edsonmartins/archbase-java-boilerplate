package br.com.archbase.boilerplate.core.infrastructure.output.persistence.mapper;

import br.com.archbase.boilerplate.core.application.dto.ProdutoCreateDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoUpdateDTO;
import br.com.archbase.boilerplate.core.domain.entity.Produto;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.ProdutoEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversão entre Produto (domínio), ProdutoEntity (persistência) e DTOs.
 */
@Mapper(componentModel = "spring")
public interface ProdutoPersistenceMapper {

    // ============ ENTITY <-> DOMAIN ============

    /**
     * Converte entidade de domínio para entidade de persistência.
     */
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createEntityDate", ignore = true)
    @Mapping(target = "updateEntityDate", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "lastModifiedByUser", ignore = true)
    @Mapping(target = "dataCadastro", source = "dataCriacao")
    ProdutoEntity toEntity(Produto produto);

    /**
     * Converte entidade de persistência para entidade de domínio.
     */
    @Mapping(target = "dataCriacao", source = "dataCadastro")
    @Mapping(target = "dataAtualizacao", source = "updateEntityDate")
    Produto toDomain(ProdutoEntity entity);

    /**
     * Atualiza entidade de persistência com dados do domínio.
     * Propriedades nulas são ignoradas.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createEntityDate", ignore = true)
    @Mapping(target = "updateEntityDate", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "lastModifiedByUser", ignore = true)
    @Mapping(target = "dataCadastro", source = "dataCriacao")
    void updateEntityFromDomain(Produto produto, @MappingTarget ProdutoEntity entity);

    /**
     * Converte lista de entidades de persistência para lista de domínio.
     */
    List<Produto> toDomainList(List<ProdutoEntity> entities);

    // ============ DOMAIN <-> DTO ============

    /**
     * Converte entidade de domínio para DTO.
     */
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createEntityDate", ignore = true)
    @Mapping(target = "updateEntityDate", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "lastModifiedByUser", ignore = true)
    @Mapping(target = "dataCadastro", source = "dataCriacao")
    ProdutoDTO toDTO(Produto produto);

    /**
     * Converte DTO para entidade de domínio.
     */
    @Mapping(target = "dataCriacao", source = "dataCadastro")
    @Mapping(target = "dataAtualizacao", source = "updateEntityDate")
    Produto toDomain(ProdutoDTO dto);

    /**
     * Converte CreateDTO para entidade de domínio.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "destaque", constant = "false")
    Produto toDomain(ProdutoCreateDTO createDTO);

    /**
     * Atualiza entidade de domínio com dados do UpdateDTO.
     * Propriedades nulas são ignoradas.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateDomainFromDTO(ProdutoUpdateDTO updateDTO, @MappingTarget Produto produto);

    /**
     * Converte lista de domínio para lista de DTOs.
     */
    List<ProdutoDTO> toDTOList(List<Produto> produtos);

    // ============ ENTITY <-> DTO ============

    /**
     * Converte entidade de persistência diretamente para DTO.
     */
    ProdutoDTO entityToDTO(ProdutoEntity entity);

    /**
     * Converte DTO diretamente para entidade de persistência.
     */
    @Mapping(target = "dataCadastro", source = "dataCadastro")
    ProdutoEntity dtoToEntity(ProdutoDTO dto);

    /**
     * Converte lista de entidades para lista de DTOs.
     */
    List<ProdutoDTO> entityListToDTOList(List<ProdutoEntity> entities);

    // ============ PAGE CONVERSIONS ============

    /**
     * Converte Page de entidades para Page de domínio.
     */
    default Page<Produto> toDomainPage(Page<ProdutoEntity> entityPage) {
        List<Produto> domainList = entityPage.getContent().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(domainList, entityPage.getPageable(), entityPage.getTotalElements());
    }

    /**
     * Converte Page de domínio para Page de DTOs.
     */
    default Page<ProdutoDTO> toDTOPage(Page<Produto> domainPage) {
        List<ProdutoDTO> dtoList = domainPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, domainPage.getPageable(), domainPage.getTotalElements());
    }

    /**
     * Converte Page de entidades diretamente para Page de DTOs.
     */
    default Page<ProdutoDTO> entityPageToDTOPage(Page<ProdutoEntity> entityPage) {
        List<ProdutoDTO> dtoList = entityPage.getContent().stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, entityPage.getPageable(), entityPage.getTotalElements());
    }
}
