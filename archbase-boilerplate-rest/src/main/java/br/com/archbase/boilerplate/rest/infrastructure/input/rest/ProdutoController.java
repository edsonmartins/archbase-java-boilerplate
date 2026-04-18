package br.com.archbase.boilerplate.rest.infrastructure.input.rest;

import br.com.archbase.boilerplate.core.application.service.ProdutoService;
import br.com.archbase.boilerplate.core.domain.dto.ProdutoCreateDTO;
import br.com.archbase.boilerplate.core.domain.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.domain.dto.ProdutoUpdateDTO;
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

/**
 * Controller REST para gerenciamento de Produtos.
 * Segue os padrões dos projetos vendax-promoter-api e gestor-rq-api.
 */
@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "Produtos", description = "Gerenciamento de Produtos")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProdutoController {

    private final ProdutoService service;

    // ===========================
    // CRUD Operations
    // ===========================

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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "409", description = "SKU já existe")
    })
    public ResponseEntity<ProdutoDTO> atualizar(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id,
            @Valid @RequestBody ProdutoUpdateDTO dto) {
        log.info("Atualizando produto: id={}", id);
        ProdutoDTO updated = service.atualizar(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar produto por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<ProdutoDTO> buscarPorId(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id) {
        log.debug("Buscando produto por ID: {}", id);
        ProdutoDTO produto = service.buscarPorId(id);
        return produto != null ? ResponseEntity.ok(produto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Buscar produto por SKU")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<ProdutoDTO> buscarPorSku(
            @Parameter(description = "SKU do produto", required = true) @PathVariable String sku) {
        log.debug("Buscando produto por SKU: {}", sku);
        ProdutoDTO produto = service.buscarPorSku(sku);
        return produto != null ? ResponseEntity.ok(produto) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Produto removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id) {
        log.info("Removendo produto: {}", id);
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    // ===========================
    // Business Operations
    // ===========================

    @PostMapping("/{id}/ativar")
    @Operation(summary = "Ativar produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto ativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<ProdutoDTO> ativar(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id) {
        log.info("Ativando produto: {}", id);
        ProdutoDTO produto = service.ativar(id);
        return ResponseEntity.ok(produto);
    }

    @PostMapping("/{id}/inativar")
    @Operation(summary = "Inativar produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<ProdutoDTO> inativar(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id) {
        log.info("Inativando produto: {}", id);
        ProdutoDTO produto = service.inativar(id);
        return ResponseEntity.ok(produto);
    }

    @PatchMapping("/{id}/estoque")
    @Operation(summary = "Atualizar estoque do produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estoque atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<ProdutoDTO> atualizarEstoque(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id,
            @Parameter(description = "Nova quantidade em estoque", required = true) @RequestParam Integer quantidade) {
        log.info("Atualizando estoque do produto {}: {}", id, quantidade);
        ProdutoDTO produto = service.atualizarEstoque(id, quantidade);
        return ResponseEntity.ok(produto);
    }

    // ===========================
    // Query Operations
    // ===========================

    @GetMapping("/categoria/{categoria}")
    @Operation(summary = "Buscar produtos por categoria")
    public ResponseEntity<List<ProdutoDTO>> buscarPorCategoria(
            @Parameter(description = "Categoria do produto", required = true) @PathVariable CategoriaProduto categoria) {
        log.debug("Buscando produtos por categoria: {}", categoria);
        List<ProdutoDTO> produtos = service.buscarPorCategoria(categoria);
        return produtos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(produtos);
    }

    @GetMapping("/ativos")
    @Operation(summary = "Buscar produtos ativos")
    public ResponseEntity<List<ProdutoDTO>> buscarAtivos() {
        log.debug("Buscando produtos ativos");
        List<ProdutoDTO> produtos = service.buscarAtivos();
        return produtos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(produtos);
    }

    // ===========================
    // Pagination Operations
    // ===========================

    @GetMapping(value = "/findAll", params = {"page", "size"})
    @Operation(summary = "Listar todos os produtos com paginação")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProdutoDTO> findAll(
            @Parameter(description = "Número da página (0-indexed)", example = "0") @RequestParam("page") int page,
            @Parameter(description = "Tamanho da página", example = "20") @RequestParam("size") int size) {
        log.debug("Buscando todos os produtos - página: {}, tamanho: {}", page, size);
        return service.buscarTodos(page, size);
    }

    @GetMapping(value = "/findAll", params = {"page", "size", "sort"})
    @Operation(summary = "Listar todos os produtos com paginação e ordenação")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProdutoDTO> findAll(
            @Parameter(description = "Número da página (0-indexed)", example = "0") @RequestParam("page") int page,
            @Parameter(description = "Tamanho da página", example = "20") @RequestParam("size") int size,
            @Parameter(description = "Ordenação (ex: nome,desc)", example = "nome,asc") @RequestParam("sort") String[] sort) {
        log.debug("Buscando todos os produtos com ordenação - página: {}, tamanho: {}", page, size);
        return service.buscarTodos(page, size, sort);
    }
}
