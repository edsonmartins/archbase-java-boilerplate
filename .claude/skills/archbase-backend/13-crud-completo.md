# 13. CRUD Completo - O Produto, passo a passo

O CRUD de **Produto** é a fonte de verdade do boilerplate. Este arquivo descreve o que o código
faz **hoje**, camada por camada, para quem for copiar o padrão para outro recurso.

Pacotes base: `br.com.archbase.boilerplate.core` (módulo `archbase-boilerplate-core`) e
`br.com.archbase.boilerplate.rest` (módulo `archbase-boilerplate-rest`).

---

## Mapa do fluxo real

```
ProdutoController (rest)
      │  chama direto
      ▼
ProdutoService (core.application.service)      ← @Service concreto, regras + @Transactional
      │  usa direto
      ▼
ProdutoJpaRepository (ArchbaseCommonJpaRepository)
      │
      ▼
ProdutoEntity (TenantPersistenceEntityBase)  →  .toDTO() / fromDTO()  →  ProdutoDTO
```

**Importante — o que existe mas NÃO está no fluxo:** `ProdutoPersistencePort`,
`ProdutoPersistenceAdapter`, `ProdutoPersistenceMapper` (MapStruct) e a entidade de domínio `Produto`
compilam e estão prontos (inclusive filtro RSQL via `FindDataWithFilterQuery` e estatísticas via
QueryDSL), mas **nenhuma classe os injeta**. Hoje o `ProdutoService` fala com o `ProdutoJpaRepository`
e converte com `ProdutoEntity.toDTO()`/`fromDTO()`. Não há pacote `port.in` nem use cases.

Ao criar um recurso novo seguindo o padrão atual: Entity + JpaRepository + DTOs + Service +
Controller. Port/Adapter/Mapper/Domain são o caminho hexagonal completo, a ser usado de propósito
(ver `05`, `06`, `07`, `10`), não por inércia.

---

## Passo 1: Domain Object (opcional no fluxo atual)

**Arquivo**: `core/domain/entity/Produto.java` — objeto puro, com Lombok
(`@Data @Builder @NoArgsConstructor @AllArgsConstructor`), `CategoriaProduto categoria`
(enum), datas `dataCriacao`/`dataAtualizacao`, `tenantId`, e comportamento (`ativar()`, `desativar()`,
`adicionarEstoque`, `removerEstoque`, `atualizarPreco`) mais a fábrica `Produto.create(...)`.

Enum `core/domain/enums/CategoriaProduto`: `ELETRONICOS, MOVEIS, ROUPAS, VESTUARIO, ALIMENTOS,
BEBIDAS, LIMPEZA, HIGIENE, ESPORTES, LIVROS, OUTROS`.

Exceções em `core/domain/exception` (ver `14-infraestrutura.md`): `BoilerplateException`,
`EntityNotFoundException`, `DuplicateEntityException`, `BusinessValidationException`.

---

## Passo 2: DTOs

**Pacote**: `core/application/dto` — `ProdutoCreateDTO`, `ProdutoUpdateDTO`, `ProdutoDTO`,
`ProdutoEstatisticasDTO`. Todos Lombok (`@Data @Builder @NoArgsConstructor @AllArgsConstructor`).
Os nomes são `ProdutoCreateDTO`/`ProdutoUpdateDTO` (sufixo, não `CreateProdutoDTO`).

```java
public class ProdutoCreateDTO {

    @NotBlank(message = "O nome e obrigatorio")
    @Size(min = 2, max = 200, message = "O nome deve ter entre 2 e 200 caracteres")
    private String nome;

    @Size(max = 1000) private String descricao;

    @NotNull @Positive @Digits(integer = 8, fraction = 2)
    private BigDecimal preco;

    @PositiveOrZero private Integer estoque;

    @NotNull(message = "A categoria e obrigatoria")
    private CategoriaProduto categoria;

    @Size(max = 100) private String sku;
    @Size(max = 100) private String marca;
    @Size(max = 500) private String urlImagem;
    private Boolean destaque;
}
```

- `ProdutoUpdateDTO`: mesmos campos, **todos opcionais** (validações só se o valor vier) e com
  `ativo`. O service aplica "só campos não nulos".
- `ProdutoDTO` (resposta): inclui os campos de auditoria da base Archbase (`id`, `code`, `version`,
  `createEntityDate`, `createdByUser`, `updateEntityDate`, `lastModifiedByUser`, `tenantId`) mais os de
  negócio e `dataCadastro`; datas com `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")`.

---

## Passo 3: Entity JPA

**Arquivo**: `core/infrastructure/output/persistence/entity/ProdutoEntity.java`

Estende **`TenantPersistenceEntityBase`** (`br.com.archbase.ddd.domain.base`), que já traz `id`,
`code`, `version`, `createEntityDate`, `createdByUser`, `updateEntityDate`, `lastModifiedByUser` e
`tenantId`. Por isso as colunas na V1 são `dh_criacao`, `dh_atualizacao`, `versao`, `codigo`.

```java
@Entity
@Table(name = "produto", indexes = {
        @Index(name = "idx_produto_nome", columnList = "nome"),
        @Index(name = "idx_produto_sku", columnList = "sku", unique = true),
        @Index(name = "idx_produto_categoria", columnList = "categoria"),
        @Index(name = "idx_produto_ativo", columnList = "ativo")
})
@Getter @Setter
public class ProdutoEntity extends TenantPersistenceEntityBase {

    @Column(name = "nome", nullable = false, length = 200) private String nome;
    @Column(name = "preco", precision = 10, scale = 2)     private BigDecimal preco;
    @Enumerated(EnumType.STRING) @Column(name = "categoria", length = 30)
    private CategoriaProduto categoria;
    @Column(name = "sku", unique = true, length = 100)      private String sku;
    // descricao, estoque, ativo, dataCadastro, destaque, urlImagem, marca ...

    public ProdutoEntity() { super(); this.ativo = true; this.estoque = 0; this.destaque = false; }

    @Builder
    public ProdutoEntity(String id, String code, Long version, LocalDateTime createEntityDate,
                         String createdByUser, LocalDateTime updateEntityDate,
                         String lastModifiedByUser, String tenantId, String nome, /* ... */) {
        super(id != null ? id : java.util.UUID.randomUUID().toString(),
              code, version,
              createEntityDate != null ? createEntityDate : LocalDateTime.now(),
              createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        // ... atribui campos com defaults (estoque 0, ativo true, destaque false)
    }

    public static ProdutoEntity fromDTO(ProdutoDTO dto) { ... }
    public ProdutoDTO toDTO() { ... }
}
```

**Armadilhas reais (já corrigidas no código — não repita):**
- O `@Builder` precisa gerar o `id` (UUID) e a `createEntityDate`: só o construtor **sem argumentos**
  da base do Archbase gera UUID. Sem isso, `POST` falhava com *"Identifier of entity ... must be
  manually assigned before calling 'persist()'"*.
- As datas do Archbase são preenchidas por construtor, **não** por `@CreatedDate`/`@LastModifiedDate`.
  Quem atualiza deve fazer `entity.setUpdateEntityDate(LocalDateTime.now())` (o `ProdutoService` faz).
- `toDTO()`/`fromDTO()` devem mapear todos os campos da entidade; ao adicionar um campo, atualize os dois
  métodos (o `ProdutoEntityTest` cobre a ida e volta de `marca`, `urlImagem` e `destaque`).

---

## Passo 4: Repository

**Arquivo**: `core/infrastructure/output/persistence/repository/ProdutoJpaRepository.java`

```java
import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;

@Repository
public interface ProdutoJpaRepository extends ArchbaseCommonJpaRepository<ProdutoEntity, String, Long> {

    Optional<ProdutoEntity> findBySku(String sku);
    boolean existsBySku(String sku);
    List<ProdutoEntity> findByCategoria(CategoriaProduto categoria);
    List<ProdutoEntity> findByAtivoTrue();
}
```

`ArchbaseCommonJpaRepository` está em `...ddd.infraestructure.persistence.jpa.repository` (note o
"infraestructure" com "e"). A classe da aplicação registra `repositoryBaseClass =
CommonArchbaseJpaRepository.class` em `@EnableJpaRepositories` (nomes parecidos, classes diferentes:
`Common...` na base, `...Common` na interface).

---

## Passo 5: Service

**Arquivo**: `core/application/service/ProdutoService.java` — `@Service @RequiredArgsConstructor
@Slf4j`, injeta apenas `ProdutoJpaRepository`.

Métodos: `criar(ProdutoCreateDTO)`, `criar(ProdutoDTO)`, `atualizar(String, ProdutoUpdateDTO)`,
`atualizar(String, ProdutoDTO)`, `buscarPorId`, `buscarPorSku`, `buscarTodos(page, size[, sort])`,
`buscarPorCategoria`, `buscarAtivos`, `remover`, `atualizarEstoque`, `ativar`, `inativar`.
As sobrecargas com `ProdutoDTO` são mantidas "por compatibilidade".

```java
@Transactional
public ProdutoDTO criar(ProdutoCreateDTO dto) {
    if (dto.getSku() != null && repository.existsBySku(dto.getSku())) {
        throw new DuplicateEntityException("Produto", "SKU", dto.getSku());     // -> 409
    }
    ProdutoEntity entity = ProdutoEntity.builder()
            .nome(dto.getNome())
            /* ... demais campos, com defaults ... */
            .ativo(true)
            .dataCadastro(LocalDateTime.now())
            .build();
    return repository.save(entity).toDTO();
}

@Transactional
public ProdutoDTO atualizar(String id, ProdutoUpdateDTO dto) {
    ProdutoEntity entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Produto", id));     // -> 404
    // se o SKU mudou, confere duplicidade; só atualiza campos não nulos
    entity.setUpdateEntityDate(LocalDateTime.now());   // as datas do Archbase não são automáticas
    return repository.save(entity).toDTO();
}

public ProdutoDTO buscarPorId(String id) {
    return repository.findById(id).map(ProdutoEntity::toDTO).orElse(null);   // null, não exceção
}
```

Convenções do service atual: erros de negócio são **exceções de domínio** (`DuplicateEntityException`,
`EntityNotFoundException`), nunca `ResponseEntity`; escritas são `@Transactional`; leituras
`buscarPorId`/`buscarPorSku` devolvem `null` e o controller converte em 404.

---

## Passo 6: Controller

**Arquivo**: `rest/infrastructure/input/rest/ProdutoController.java` — chama o `ProdutoService`
direto, sem use cases, e **sem** `@RequireRole`/`@RequireProfile`/`@HasPermission`; só
`@SecurityRequirement(name = "bearerAuth")` para o Swagger. Detalhes e tabela de rotas em
`09-controller.md`.

```java
@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "Produtos", description = "Gerenciamento de Produtos")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProdutoController {

    private final ProdutoService service;

    @PostMapping
    public ResponseEntity<ProdutoDTO> criar(@Valid @RequestBody ProdutoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }
    // PUT /{id}, GET /{id}, GET /sku/{sku}, DELETE /{id}, POST /{id}/ativar, POST /{id}/inativar,
    // PATCH /{id}/estoque, GET /categoria/{categoria}, GET /ativos, GET /findAll?page=&size=[&sort=]
}
```

---

## Passo 7: Migration

**Arquivo**: `rest/src/main/resources/db/migration/V1__schema_inicial.sql` — cria `produto` e as
tabelas `seguranca_*` do Archbase. Em produção o Hibernate roda com `ddl-auto: validate`; sem a
tabela do novo recurso numa migration, a aplicação **não sobe**. Ao criar entidade nova, escreva
`V2__...sql` (ver `14-infraestrutura.md`). Colunas herdadas da base: `id`, `codigo`, `versao`,
`dh_criacao`, `dh_atualizacao`, `tenant_id` (NOT NULL), `usuario_criou` e `ultimo_usuario_alterou`.

---

## Passo 8: Testes

- `core/src/test/.../ProdutoServiceTest` — unitário, Mockito sobre `ProdutoJpaRepository`, com
  `@Nested` por método (`criar`, `atualizar`, `buscarPorId`, `remover`, status, busca).
- `rest/src/test/.../AplicacaoSobeTest` — `@SpringBootTest` com PostgreSQL real; exercita criar/
  atualizar de verdade (pega os erros de id e de datas que o unitário não vê). Se não houver
  Postgres, o teste é pulado.

Detalhes em `14-infraestrutura.md`.

---

## Checklist para um novo recurso, copiando o Produto

1. `core/domain/enums` (se houver enum) e exceções de domínio já existentes.
2. DTOs `XxxCreateDTO`, `XxxUpdateDTO`, `XxxDTO` com Bean Validation.
3. `XxxEntity extends TenantPersistenceEntityBase` (`@Builder` gerando `id` e `createEntityDate`),
   com `toDTO()` mapeando **todos** os campos.
4. `XxxJpaRepository extends ArchbaseCommonJpaRepository<XxxEntity, String, Long>`.
5. `XxxService` com `@Transactional` nas escritas, lançando exceções de domínio, e
   `setUpdateEntityDate` nas alterações.
6. `XxxController` em `/api/v1/<recurso>`, delegando ao service; decidir se algum endpoint precisa de
   `@HasPermission` (ver `12-seguranca.md`).
7. Migration `V{n}__xxx.sql`.
8. Teste unitário do service + caso no teste de subida.

## Resumo dos arquivos do Produto

| Camada | Arquivo | No fluxo atual? |
|--------|---------|-----------------|
| Domain | `core/domain/entity/Produto.java`, `domain/enums/CategoriaProduto.java` | só via mapper (não usado) |
| Exceções | `core/domain/exception/*.java` | sim |
| DTO | `core/application/dto/Produto{,Create,Update,Estatisticas}DTO.java` | sim |
| Port (saída) | `core/application/port/out/ProdutoPersistencePort.java` | não injetado |
| Service | `core/application/service/ProdutoService.java` | sim |
| Entity | `core/infrastructure/output/persistence/entity/ProdutoEntity.java` | sim |
| Repository | `.../persistence/repository/ProdutoJpaRepository.java` | sim |
| Adapter | `.../persistence/adapter/ProdutoPersistenceAdapter.java` | não injetado |
| Mapper | `.../persistence/mapper/ProdutoPersistenceMapper.java` | não injetado |
| Controller | `rest/infrastructure/input/rest/ProdutoController.java` | sim |
| Migration | `rest/src/main/resources/db/migration/V1__schema_inicial.sql` | sim |
| Seed | `rest/seed/DataSeedLoader.java` (perfil `dev`) | sim |
| Testes | `ProdutoServiceTest`, `AplicacaoSobeTest` | sim |
