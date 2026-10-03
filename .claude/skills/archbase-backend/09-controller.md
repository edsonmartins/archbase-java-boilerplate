# 09. Controllers REST

Controllers expõem a API REST. No boilerplate, o `ProdutoController` é a referência e é ele que este
arquivo descreve.

---

## Conceito

O controller do boilerplate:
- Usa `@RestController` e `@RequestMapping("/api/v1/<recurso>")`
- **Injeta o `ProdutoService` diretamente** (não há ports de entrada / use cases neste projeto)
- Documenta com OpenAPI (`@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`)
- Valida o corpo com `@Valid` e a classe com `@Validated`
- Retorna `ResponseEntity` (ou `Page<...>` nas listagens paginadas)
- Não trata exceção: ela sobe até o `RestExceptionHandler` (ver `14-infraestrutura.md`)

> O `ProdutoService` é um `@Service` concreto, sem interface. Não existe pacote `port.in` no projeto.
> Só há porta de **saída** (`port.out.ProdutoPersistencePort`). Não invente `CreateXxxUseCase`.

---

## Segurança no controller

O `ProdutoController` tem `@SecurityRequirement(name = "bearerAuth")`. Essa anotação é do **OpenAPI**
(`io.swagger.v3.oas.annotations.security`): ela só faz o Swagger UI mostrar o cadeado e enviar o
token. **Não autoriza nada.**

O controller **não usa** `@RequireRole`, `@RequireProfile`, `@RequirePersona` nem `@HasPermission`.
Quem exige autenticação é o `archbase-security` para tudo que não estiver em
`archbase.security.whitelist` (ver `12-seguranca.md`). Qualquer usuário autenticado chama qualquer
endpoint do Produto.

---

## Controller real: ProdutoController

```java
package br.com.archbase.boilerplate.rest.infrastructure.input.rest;

import br.com.archbase.boilerplate.core.application.dto.ProdutoCreateDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoUpdateDTO;
import br.com.archbase.boilerplate.core.application.service.ProdutoService;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "Produtos", description = "Gerenciamento de Produtos")
@SecurityRequirement(name = "bearerAuth")   // só documentação OpenAPI
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProdutoController {

    private final ProdutoService service;

    @PostMapping
    @Operation(summary = "Criar novo produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProdutoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "409", description = "SKU já existe")
    })
    public ResponseEntity<ProdutoDTO> criar(@Valid @RequestBody ProdutoCreateDTO dto) {
        log.info("Criando novo produto: {}", dto.getNome());
        ProdutoDTO created = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto existente")
    public ResponseEntity<ProdutoDTO> atualizar(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id,
            @Valid @RequestBody ProdutoUpdateDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar produto por ID")
    public ResponseEntity<ProdutoDTO> buscarPorId(@PathVariable String id) {
        ProdutoDTO produto = service.buscarPorId(id);
        return produto != null ? ResponseEntity.ok(produto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProdutoDTO> buscarPorSku(@PathVariable String sku) { /* idem */ }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover produto")
    public ResponseEntity<Void> remover(@PathVariable String id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Operações de negócio ----

    @PostMapping("/{id}/ativar")
    public ResponseEntity<ProdutoDTO> ativar(@PathVariable String id) {
        return ResponseEntity.ok(service.ativar(id));
    }

    @PostMapping("/{id}/inativar")
    public ResponseEntity<ProdutoDTO> inativar(@PathVariable String id) {
        return ResponseEntity.ok(service.inativar(id));
    }

    @PatchMapping("/{id}/estoque")
    public ResponseEntity<ProdutoDTO> atualizarEstoque(
            @PathVariable String id,
            @Parameter(description = "Nova quantidade em estoque", required = true)
            @RequestParam Integer quantidade) {
        return ResponseEntity.ok(service.atualizarEstoque(id, quantidade));
    }

    // ---- Consultas ----

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ProdutoDTO>> buscarPorCategoria(@PathVariable CategoriaProduto categoria) {
        List<ProdutoDTO> produtos = service.buscarPorCategoria(categoria);
        return produtos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(produtos);
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<ProdutoDTO>> buscarAtivos() {
        List<ProdutoDTO> produtos = service.buscarAtivos();
        return produtos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(produtos);
    }

    // ---- Paginação (retorna Page<ProdutoDTO> direto, com @ResponseStatus) ----

    @GetMapping(value = "/findAll", params = {"page", "size"})
    @ResponseStatus(HttpStatus.OK)
    public Page<ProdutoDTO> findAll(@RequestParam("page") int page, @RequestParam("size") int size) {
        return service.buscarTodos(page, size);
    }

    @GetMapping(value = "/findAll", params = {"page", "size", "sort"})
    @ResponseStatus(HttpStatus.OK)
    public Page<ProdutoDTO> findAll(@RequestParam("page") int page, @RequestParam("size") int size,
                                    @RequestParam("sort") String[] sort) {
        return service.buscarTodos(page, size, sort);
    }
}
```

(Trechos abreviados com `@Operation`/`@ApiResponses` omitidos; o arquivo real documenta todos os
endpoints com `@ApiResponses`.)

---

## Tabela de endpoints

| Método | Rota | Retorno |
|--------|------|---------|
| POST | `/api/v1/produtos` | 201 + `ProdutoDTO` |
| PUT | `/api/v1/produtos/{id}` | 200 + `ProdutoDTO` |
| GET | `/api/v1/produtos/{id}` | 200, ou 404 sem corpo |
| GET | `/api/v1/produtos/sku/{sku}` | 200, ou 404 sem corpo |
| DELETE | `/api/v1/produtos/{id}` | 204 |
| POST | `/api/v1/produtos/{id}/ativar` | 200 |
| POST | `/api/v1/produtos/{id}/inativar` | 200 |
| PATCH | `/api/v1/produtos/{id}/estoque?quantidade=N` | 200 |
| GET | `/api/v1/produtos/categoria/{categoria}` | 200, ou 204 se vazio |
| GET | `/api/v1/produtos/ativos` | 200, ou 204 se vazio |
| GET | `/api/v1/produtos/findAll?page=&size=[&sort=]` | `Page<ProdutoDTO>` |

Pontos de atenção do código atual:
- Paginação é `findAll?page=&size=` (0-indexed), **não** `GET /produtos?page=`. Os dois métodos
  `findAll` se distinguem pelo atributo `params` do `@GetMapping`.
- `sort` chega como `String[]` (`sort=nome,desc`, a vírgula separa os elementos); o service só lê
  `sort[0]` (campo) e `sort[1]` (`desc`).
- `buscarPorId` e `buscarPorSku` devolvem 404 **no controller**, porque o service retorna `null`.
  Já `atualizar`, `remover`, `ativar`, `inativar` e `atualizarEstoque` lançam `EntityNotFoundException`
  no service, que o `RestExceptionHandler` converte em 404 com corpo `ApiError`.
- Listas vazias respondem 204 (`noContent`), não 200 com `[]`.

---

## Tratamento de erros

O controller **não** define `@ExceptionHandler`. O tratamento é global, no
`RestExceptionHandler` (`@ControllerAdvice`, `@Order(HIGHEST_PRECEDENCE)`) em
`rest.infrastructure.error`, que devolve o `ApiError`. Detalhes em `14-infraestrutura.md`.

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **@Valid no @RequestBody** | Valida o DTO; a falha vira 400 com `subErrors` |
| **@Tag / @Operation / @ApiResponses** | Documentar no Swagger, inclusive 404/409 |
| **@SecurityRequirement** | Só documentação OpenAPI; não protege |
| **Controller fino** | Só delega ao service e escolhe o status HTTP |
| **Sem try/catch** | Exceções de domínio sobem para o `RestExceptionHandler` |
| **@Slf4j** | `info` nas escritas, `debug` nas leituras |
