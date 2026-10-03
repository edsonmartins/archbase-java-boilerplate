# 08. Services (Camada de Aplicação)

Services contêm as regras de negócio e orquestram o CRUD.

---

## Conceito

No boilerplate o Service é uma **classe concreta** (`ProdutoService`), sem interface de use case
(não existe `port/in`). O `ProdutoController` (módulo rest) injeta o Service diretamente.

O Service real:
- É anotado com `@Service`, `@RequiredArgsConstructor`, `@Slf4j`
- Injeta o **`ProdutoJpaRepository`** (não o `ProdutoPersistencePort`)
- Trabalha com `ProdutoEntity` e DTOs (`ProdutoEntity.fromDTO` / `entity.toDTO()`), não com o domínio `Produto`
- Lança exceções de `core.domain.exception`: `DuplicateEntityException`, `EntityNotFoundException`
  (também existem `BoilerplateException` e `BusinessValidationException`)
- Não resolve tenant: o filtro é automático (`@TenantId` na entidade base)

---

## Service Real: ProdutoService

```java
package br.com.archbase.boilerplate.core.application.service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProdutoService {

    private final ProdutoJpaRepository repository;

    @Transactional
    public ProdutoDTO criar(ProdutoCreateDTO dto) {
        log.info("Criando produto: {}", dto.getNome());

        // Verificar SKU único
        if (dto.getSku() != null && repository.existsBySku(dto.getSku())) {
            throw new DuplicateEntityException("Produto", "SKU", dto.getSku());
        }

        ProdutoEntity entity = ProdutoEntity.builder()
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .preco(dto.getPreco())
                .estoque(dto.getEstoque() != null ? dto.getEstoque() : 0)
                .categoria(dto.getCategoria())
                .sku(dto.getSku())
                .marca(dto.getMarca())
                .urlImagem(dto.getUrlImagem())
                .destaque(dto.getDestaque() != null ? dto.getDestaque() : false)
                .ativo(true)
                .dataCadastro(LocalDateTime.now())
                .build();

        ProdutoEntity saved = repository.save(entity);
        return saved.toDTO();
    }
    // ...
}
```

Há também sobrecargas `criar(ProdutoDTO)` e `atualizar(String, ProdutoDTO)`, mantidas por compatibilidade.

---

## Atualização Parcial

```java
@Transactional
public ProdutoDTO atualizar(String id, ProdutoUpdateDTO dto) {
    ProdutoEntity entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Produto", id));

    // Verificar SKU se foi alterado
    if (dto.getSku() != null && !dto.getSku().equals(entity.getSku())) {
        if (repository.existsBySku(dto.getSku())) {
            throw new DuplicateEntityException("Produto", "SKU", dto.getSku());
        }
        entity.setSku(dto.getSku());
    }

    // Atualizar apenas campos não nulos
    if (dto.getNome() != null) entity.setNome(dto.getNome());
    if (dto.getPreco() != null) entity.setPreco(dto.getPreco());
    // ... demais campos ...

    // As datas do archbase não são preenchidas por @LastModifiedDate: marcar explicitamente
    entity.setUpdateEntityDate(LocalDateTime.now());

    return repository.save(entity).toDTO();
}
```

**Importante**: `PersistenceEntityBase` não usa `@LastModifiedDate`; quem altera deve chamar
`setUpdateEntityDate(LocalDateTime.now())`, senão `updateEntityDate` fica nulo (o
`lastModifiedByUser` é preenchido pelo `AuditingEntityListener`). Os métodos `ativar`, `inativar` e
`atualizarEstoque` do Service atual **não** marcam essa data.

---

## Leituras, Remoção e Status

```java
public ProdutoDTO buscarPorId(String id) {            // retorna null se não achar
    return repository.findById(id).map(ProdutoEntity::toDTO).orElse(null);
}

public Page<ProdutoDTO> buscarTodos(int page, int size) {
    return repository.findAll(PageRequest.of(page, size)).map(ProdutoEntity::toDTO);
}

public List<ProdutoDTO> buscarPorCategoria(CategoriaProduto categoria) {
    return repository.findByCategoria(categoria).stream().map(ProdutoEntity::toDTO).toList();
}

@Transactional
public void remover(String id) {
    if (!repository.existsById(id)) {
        throw new EntityNotFoundException("Produto", id);
    }
    repository.deleteById(id);
}

@Transactional
public ProdutoDTO ativar(String id) {
    ProdutoEntity entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Produto", id));
    entity.setAtivo(true);
    return repository.save(entity).toDTO();
}
```

Outros métodos: `buscarPorSku`, `buscarTodos(page, size, sort)`, `buscarAtivos`, `inativar`,
`atualizarEstoque(id, quantidade)`.

---

## Testes Unitários: ProdutoServiceTest

Mockito puro, sem Spring, com o repository mockado:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProdutoService - Testes de Serviço de Produto")
class ProdutoServiceTest {

    @Mock
    private ProdutoJpaRepository repository;

    @InjectMocks
    private ProdutoService produtoService;

    @Nested
    @DisplayName("Método criar")
    class CriarTests {

        @Test
        @DisplayName("Deve lançar exceção quando SKU já existe")
        void deveLancarExcecaoQuandoSkuJaExiste() {
            when(repository.existsBySku(createDTO.getSku())).thenReturn(true);

            assertThatThrownBy(() -> produtoService.criar(createDTO))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessageContaining("SKU");

            verify(repository, never()).save(any(ProdutoEntity.class));
        }
    }
}
```

Padrões: `@Nested` por método (`criar`, `atualizar`, `buscarPorId`, `remover`, status, busca),
`@DisplayName` em português, comentários Given/When/Then, AssertJ.

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **@Transactional** | Em métodos que escrevem dados |
| **Service concreto** | Sem interface de use case; o controller injeta o Service |
| **Exceções de domínio** | `DuplicateEntityException`, `EntityNotFoundException` |
| **SKU único** | Checar com `existsBySku` antes de salvar |
| **Datas** | Marcar `updateEntityDate` ao alterar |
| **Log** | `log.info` em escritas, `log.debug` em leituras |
| **@RequiredArgsConstructor** | Injeção via construtor |

---

**IMPORTANTE**: O Service atual acessa o repository direto e usa a entidade JPA. Para passar pelo Port/Adapter (QueryDSL, RSQL), injete `ProdutoPersistencePort` no lugar do repository.
