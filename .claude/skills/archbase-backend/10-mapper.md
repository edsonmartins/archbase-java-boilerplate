# 10. Mappers (MapStruct)

Mappers convertem entre Domain, DTO e Entity.

---

## Conceito

Mapper deve:
- Ser uma interface anotada com `@Mapper(componentModel = "spring")`
- Ficar em `infrastructure/output/persistence/mapper`
- Ser gerado pelo MapStruct (1.6.3; o `-Amapstruct.defaultComponentModel=spring` também está no `maven-compiler-plugin` do core, junto com `lombok-mapstruct-binding`)

O boilerplate tem **um** mapper: `ProdutoPersistenceMapper`. Ele é usado apenas pelo
`ProdutoPersistenceAdapter`. O `ProdutoService` não o usa; converte com `ProdutoEntity.toDTO()` /
`ProdutoEntity.fromDTO(dto)`.

---

## Mapper Real: ProdutoPersistenceMapper

```java
package br.com.archbase.boilerplate.core.infrastructure.output.persistence.mapper;

@Mapper(componentModel = "spring")
public interface ProdutoPersistenceMapper {

    // ============ ENTITY <-> DOMAIN ============

    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createEntityDate", ignore = true)
    @Mapping(target = "updateEntityDate", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "lastModifiedByUser", ignore = true)
    @Mapping(target = "dataCadastro", source = "dataCriacao")
    ProdutoEntity toEntity(Produto produto);

    @Mapping(target = "dataCriacao", source = "dataCadastro")
    @Mapping(target = "dataAtualizacao", source = "updateEntityDate")
    Produto toDomain(ProdutoEntity entity);

    List<Produto> toDomainList(List<ProdutoEntity> entities);

    // ============ DOMAIN <-> DTO ============

    @Mapping(target = "dataCadastro", source = "dataCriacao")
    // + ignores de code/version/datas/usuários de auditoria
    ProdutoDTO toDTO(Produto produto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "destaque", constant = "false")
    Produto toDomain(ProdutoCreateDTO createDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateDomainFromDTO(ProdutoUpdateDTO updateDTO, @MappingTarget Produto produto);

    // ============ ENTITY <-> DTO ============

    ProdutoDTO entityToDTO(ProdutoEntity entity);
    List<ProdutoDTO> entityListToDTOList(List<ProdutoEntity> entities);
}
```

Existem ainda `updateEntityFromDomain(Produto, @MappingTarget ProdutoEntity)`, `dtoToEntity`,
`toDomain(ProdutoDTO)` e `toDTOList`.

---

## Por que tantos @Mapping(ignore = true)

`ProdutoEntity` herda de `TenantPersistenceEntityBase` -> `PersistenceEntityBase`, que traz
`code`, `version`, `createEntityDate`, `updateEntityDate`, `createdByUser`, `lastModifiedByUser`
(e `tenantId`). O domínio `Produto` não tem esses campos; ao mapear Domain -> Entity eles são
ignorados (versão, auditoria e tenant são do framework).

Nomes que diferem entre as camadas:

| Domínio (`Produto`) | Entity / DTO |
|---------------------|--------------|
| `dataCriacao` | `dataCadastro` |
| `dataAtualizacao` | `updateEntityDate` |

---

## Atualização Parcial (@BeanMapping + @MappingTarget)

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateEntityFromDomain(Produto produto, @MappingTarget ProdutoEntity entity);
```

`IGNORE` faz propriedades nulas não sobrescreverem o destino (semântica de PATCH).

---

## Constantes e Page

```java
@Mapping(target = "ativo", constant = "true")     // valor fixo no mapeamento
```

Conversão de `Page` é feita com métodos `default` na própria interface:

```java
default Page<Produto> toDomainPage(Page<ProdutoEntity> entityPage) {
    List<Produto> domainList = entityPage.getContent().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    return new PageImpl<>(domainList, entityPage.getPageable(), entityPage.getTotalElements());
}
```

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **componentModel = "spring"** | Gera bean Spring |
| **ignore de campos de base** | `code`, `version`, datas e usuários de auditoria, `tenantId` |
| **@Mapping para nomes diferentes** | `dataCriacao` <-> `dataCadastro` |
| **IGNORE em nulos** | Para atualização parcial |
| **Listas automáticas** | MapStruct gera `List<X>` a partir do método de elemento |
| **default methods** | Para `Page` |

---

**IMPORTANTE**: Não existe um mapper separado em `application/mapper`; Entity <-> Domain <-> DTO ficam todos no `ProdutoPersistenceMapper`. Se um mapping ficar sem `ignore` para campo que não existe no destino, o MapStruct avisa na compilação.
