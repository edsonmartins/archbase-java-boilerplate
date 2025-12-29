# 09. Controllers REST

Controllers expõem a API REST, injetando Use Cases.

---

## Conceito

**CRÍTICO**: Controller deve:
- Injetar Use Cases (Ports de Entrada), não Services diretamente
- Usar `@RestController` e `@RequestMapping`
- Ter endpoints documentados com OpenAPI
- Usar `@Valid` para validação
- Retornar `ResponseEntity` com tipos apropriados

---

## Controller CRUD Básico

```java
package br.com.empresa.projeto.rest.infrastructure.input.rest;

import br.com.empresa.projeto.core.application.dto.*;
import br.com.empresa.projeto.core.application.port.in.*;
import br.com.archbase.multitenancy.ArchbaseCurrentTenantIdentifierResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
@Tag(name = "Examples", description = "Gerenciamento de Examples")
public class ExampleController {

    private final CreateExampleUseCase createUseCase;
    private final UpdateExampleUseCase updateUseCase;
    private final DeleteExampleUseCase deleteUseCase;
    private final FindExampleUseCase findUseCase;

    @PostMapping
    @Operation(summary = "Criar novo example", description = "Cria um novo example no sistema")
    public ResponseEntity<ExampleDTO> create(
            @Parameter(description = "Dados do example", required = true)
            @Valid @RequestBody CreateExampleDTO dto) {

        ExampleDTO created = createUseCase.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar example por ID", description = "Retorna um example específico")
    public ResponseEntity<ExampleDTO> findById(
            @Parameter(description = "ID do example", required = true, example = "123")
            @PathVariable String id) {

        ExampleDTO dto = findUseCase.findById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @Operation(summary = "Listar todos os examples", description = "Retorna paginação de examples")
    public ResponseEntity<PageResponseDTO<ExampleDTO>> findAll(
            @Parameter(description = "Número da página", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamanho da página", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        PageResponseDTO<ExampleDTO> result = findUseCase.findAll(page, size);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar example", description = "Atualiza dados de um example")
    public ResponseEntity<ExampleDTO> update(
            @Parameter(description = "ID do example", required = true)
            @PathVariable String id,

            @Parameter(description = "Dados para atualização", required = true)
            @Valid @RequestBody UpdateExampleDTO dto) {

        ExampleDTO updated = updateUseCase.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar example", description = "Remove um example do sistema")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID do example", required = true)
            @PathVariable String id) {

        deleteUseCase.delete(id);
    }
}
```

---

## Controller com Busca Avançada

```java
@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos")
public class ProdutoController {

    private final ProdutoUseCase produtoUseCase;

    @GetMapping("/buscar")
    @Operation(summary = "Buscar produtos com filtros")
    public ResponseEntity<PageResponseDTO<ProdutoDTO>> buscar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ProdutoFilterDTO filter = new ProdutoFilterDTO();
        filter.setNome(nome);
        filter.setSku(sku);
        filter.setCategoria(categoria);
        filter.setAtivo(ativo);
        filter.setPrecoMin(precoMin);
        filter.setPrecoMax(precoMax);

        PageResponseDTO<ProdutoDTO> result = produtoUseCase.buscar(filter, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorias de produtos")
    public ResponseEntity<List<CategoriaDTO>> getCategorias() {
        return ResponseEntity.ok(produtoUseCase.getCategorias());
    }

    @GetMapping("/ativos")
    @Operation(summary = "Listar produtos ativos")
    public ResponseEntity<List<ProdutoDTO>> getAtivos() {
        return ResponseEntity.ok(produtoUseCase.getAtivos());
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar produto")
    public ResponseEntity<ProdutoDTO> ativar(@PathVariable String id) {
        return ResponseEntity.ok(produtoUseCase.ativar(id));
    }

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativar produto")
    public ResponseEntity<ProdutoDTO> desativar(@PathVariable String id) {
        return ResponseEntity.ok(produtoUseCase.desativar(id));
    }
}
```

---

## Controller para Resource Aninhado

```java
@RestController
@RequestMapping("/api/v1/clientes/{clienteId}/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos")
public class ClientePedidoController {

    private final PedidoUseCase pedidoUseCase;

    @GetMapping
    @Operation(summary = "Listar pedidos do cliente")
    public ResponseEntity<List<PedidoDTO>> listarPorCliente(
            @PathVariable String clienteId) {

        return ResponseEntity.ok(pedidoUseCase.listarPorCliente(clienteId));
    }

    @PostMapping
    @Operation(summary = "Criar pedido para o cliente")
    public ResponseEntity<PedidoDTO> criar(
            @PathVariable String clienteId,
            @Valid @RequestBody CreatePedidoDTO dto) {

        dto.setClienteId(clienteId);
        PedidoDTO created = pedidoUseCase.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

---

## Controller com Tratamento de Erros

```java
@RestControllerAdvice
@Tag(name = "Tratamento de Exceções")
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ValidationErrorResponse response = new ValidationErrorResponse();
        response.setMessage("Erro de validação");
        response.setErrors(errors);
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Erro não tratado", ex);

        ErrorResponse error = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("Erro interno do servidor")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## Response Padrão

```java
@Schema(description = "Response de erro")
public class ErrorResponse {

    @Schema(description = "Código do erro")
    private String code;

    @Schema(description = "Mensagem do erro")
    private String message;

    @Schema(description = "Timestamp do erro")
    private LocalDateTime timestamp;

    @Builder
    public ErrorResponse(String code, String message, LocalDateTime timestamp) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters...
}
```

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **Injetar Use Cases** | Não injetar Services diretamente |
| **@Valid** | Validar DTOs automaticamente |
| **@Tag** | Documentar endpoints no Swagger |
| **@Operation** | Descrever cada endpoint |
| **@Parameter** | Documentar parâmetros |
| **ResponseEntity** | Retornar com status code correto |
| **@RestControllerAdvice** | Tratamento centralizado de erros |

---

**IMPORTANTE**: Controllers são "finos" - apenas orquestram chamadas aos Use Cases.
