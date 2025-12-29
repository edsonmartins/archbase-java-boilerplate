# 10. Mappers (MapStruct)

Mappers convertem entre Domain, DTO e Entity.

---

## Conceito

**CRÍTICO**: Mapper deve:
- Ser uma interface anotada com `@Mapper`
- Usar `componentModel = "spring"`
- Ter métodos de conversão bidirecionais
- Ser gerado automaticamente pelo MapStruct

---

## Mapper Básico

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.mapper;

import br.com.empresa.projeto.core.application.dto.ExampleDTO;
import br.com.empresa.projeto.core.application.dto.CreateExampleDTO;
import br.com.empresa.projeto.core.domain.entity.Example;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.ExampleEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    uses = {} // outros mappers se necessário
)
public interface ExampleMapper {

    // Domain → DTO
    ExampleDTO toDTO(Example domain);

    // DTO → Domain
    Example toDomain(CreateExampleDTO dto);

    // Domain → Entity
    ExampleEntity toEntity(Example domain);

    // Entity → Domain
    Example toDomain(ExampleEntity entity);

    // Entity → DTO (para queries diretas)
    ExampleDTO entityToDTO(ExampleEntity entity);

    // Listas
    List<ExampleDTO> toDTOList(List<Example> domains);
    List<Example> toDomainList(List<ExampleEntity> entities);
}
```

---

## Mapper com @Mapping

```java
package br.com.empresa.projeto.core.infrastructure.output.persistence.mapper;

import br.com.empresa.projeto.core.application.dto.ProdutoDTO;
import br.com.empresa.projeto.core.domain.entity.Produto;
import br.com.empresa.projeto.core.infrastructure.output.persistence.entity.ProdutoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createEntityDate", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "updateEntityDate", ignore = true)
    @Mapping(target = "lastModifiedByUser", ignore = true)
    @Mapping(target = "version", ignore = true)
    ProdutoEntity toEntityForCreate(Produto domain);

    @Mapping(source = "categoria", target = "categoriaNome")
    ProdutoDTO toDTO(Produto domain);

    @Mapping(source = "tenantId", target = "tenantId")
    Produto toDomain(ProdutoEntity entity);
}
```

---

## Mapper com Conversões Customizadas

```java
@Mapper(
    componentModel = "spring",
    uses = {CategoriaMapper.class, LocalDateTimeMapper.class}
)
public interface ProdutoMapper {

    @Mapping(target = "categoria", source = "categoriaDto")
    Produto toDomain(ProdutoDTO dto);

    @Mapping(target = "categoriaDto", source = "categoria")
    ProdutoDTO toDTO(Produto domain);
}

// Mapper auxiliar
@Component
public class CategoriaMapper {

    public Categoria toEntity(CategoriaDTO dto) {
        if (dto == null) return null;
        Categoria entity = new Categoria();
        entity.setId(dto.getId());
        entity.setNome(dto.getNome());
        return entity;
    }

    public CategoriaDTO toDTO(Categoria entity) {
        if (entity == null) return null;
        return new CategoriaDTO(entity.getId(), entity.getNome());
    }
}
```

---

## Mapper com Métodos Customizados (@Named)

```java
@Mapper(componentModel = "spring")
public interface ExampleMapper {

    @Mapping(target = "status", source = "ativo", qualifiedByName = "ativoToStatus")
    ExampleDTO toDTO(Example domain);

    @Mapping(target = "ativo", source = "status", qualifiedByName = "statusToAtivo")
    Example toDomain(ExampleDTO dto);

    @Named("ativoToStatus")
    default String ativoToStatus(Boolean ativo) {
        return Boolean.TRUE.equals(ativo) ? "ATIVO" : "INATIVO";
    }

    @Named("statusToAtivo")
    default Boolean statusToAtivo(String status) {
        return "ATIVO".equals(status);
    }

    @Named("stringToLocalDate")
    default LocalDate stringToLocalDate(String data) {
        if (data == null) return null;
        return LocalDate.parse(data);
    }

    @Named("localDateToString")
    default String localDateToString(LocalDate data) {
        if (data == null) return null;
        return data.toString();
    }
}
```

---

## Mapper para Collections

```java
@Mapper(componentModel = "spring")
public interface PedidoMapper {

    PedidoDTO toDTO(Pedido domain);
    Pedido toDomain(PedidoDTO dto);

    PedidoEntity toEntity(Pedido domain);
    Pedido toDomain(PedidoEntity entity);

    // Listas - MapStruct gera automaticamente
    List<PedidoDTO> toDTOList(List<Pedido> domains);
    List<Pedido> toDomainList(List<PedidoEntity> entities);

    // Sets
    Set<PedidoDTO> toDTOSet(Set<Pedido> domains);

    // Mapeamento aninhado
    @Mapping(target = "itens", source = "itens")
    PedidoDTO toDTOComItens(Pedido domain);
}
```

---

## Mapper com Ignore

```java
@Mapper(componentModel = "spring")
public interface CreateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createEntityDate", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "updateEntityDate", ignore = true)
    @Mapping(target = "lastModifiedByUser", ignore = true)
    @Mapping(target = "version", ignore = true)
    ExampleEntity toEntityForCreate(Example domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    ExampleEntity toEntityForUpdate(Example domain);
}
```

---

## Mapper Expressão

```java
@Mapper(componentModel = "spring", imports = {UUID.class})
public interface ExampleMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "createEntityDate", expression = "java(java.time.LocalDateTime.now())")
    ExampleEntity toNewEntity(Example domain);

    @Mapping(target = "nomeCompleto",
             expression = "java(entity.getNome() + \" \" + entity.getSobrenome())")
    ExampleDTO toDTO(ExampleEntity entity);
}
```

---

## Ciclo de Vida Domain ↔ Entity ↔ DTO

```
┌──────────────────────────────────────────────────────────────┐
│  REQUEST (JSON)                                              │
└───────────────────┬──────────────────────────────────────────┘
                    │ @RequestBody @Valid
                    ▼
┌──────────────────────────────────────────────────────────────┐
│  CreateDTO                                                   │
└───────────────────┬──────────────────────────────────────────┘
                    │ mapper.toDomain(dto)
                    ▼
┌──────────────────────────────────────────────────────────────┐
│  Domain (Example)                                            │
└───────────────────┬──────────────────────────────────────────┘
                    │ mapper.toEntity(domain)
                    ▼
┌──────────────────────────────────────────────────────────────┐
│  JPA Entity (ExampleEntity)                                  │
└───────────────────┬──────────────────────────────────────────┘
                    │ repository.save()
                    ▼
┌──────────────────────────────────────────────────────────────┐
│  Database                                                    │
└──────────────────────────────────────────────────────────────┘
```

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **componentModel = "spring"** | Gera bean Spring |
| **injectionStrategy = CONSTRUCTOR** | Injeção via construtor |
| **@Mapping para campos diferentes** | Nomes diferentes source/target |
| **ignore campos calculados** | id, tenantId, datas de auditoria |
| **Listas automáticas** | MapStruct gera para collections |
| **@Named para métodos customizados** | Reutilizar conversões complexas |

---

**CRÍTICO**: Não faça mapeamento manual. Use sempre MapStruct com as anotações corretas.
