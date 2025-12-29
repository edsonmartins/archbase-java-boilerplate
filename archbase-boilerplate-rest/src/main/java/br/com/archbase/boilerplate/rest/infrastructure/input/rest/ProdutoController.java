package br.com.archbase.boilerplate.rest.infrastructure.input.rest;

import br.com.archbase.boilerplate.core.application.service.ProdutoService;
import br.com.archbase.boilerplate.core.domain.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 */
@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "Produtos", description = "Operações relacionadas ao gerenciamento de produtos")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProdutoController {

    private final ProdutoService service;

    // ===========================
    // CRUD Operations
    // ===========================

    @Operation(summary = "Criar novo produto")
    @PostMapping
    public ResponseEntity<ProdutoDTO> criar(@Valid @RequestBody ProdutoDTO dto) {
        try {
            log.info("Criando novo produto: {}", dto.getNome());
            ProdutoDTO created = service.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao criar produto: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao criar produto", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Atualizar produto existente")
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoDTO> atualizar(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id,
            @Valid @RequestBody ProdutoDTO dto) {
        try {
            log.info("Atualizando produto: id={}", id);
            ProdutoDTO updated = service.atualizar(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Produto não encontrado: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao atualizar produto {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Buscar produto por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDTO> buscarPorId(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id) {
        try {
            log.debug("Buscando produto por ID: {}", id);
            ProdutoDTO produto = service.buscarPorId(id);
            return produto != null ? ResponseEntity.ok(produto) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao buscar produto {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Buscar produto por SKU")
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProdutoDTO> buscarPorSku(
            @Parameter(description = "SKU do produto", required = true) @PathVariable String sku) {
        try {
            log.debug("Buscando produto por SKU: {}", sku);
            ProdutoDTO produto = service.buscarPorSku(sku);
            return produto != null ? ResponseEntity.ok(produto) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao buscar produto por SKU {}", sku, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Remover produto")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id) {
        try {
            log.info("Removendo produto: {}", id);
            service.remover(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover produto {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===========================
    // Business Operations
    // ===========================

    @Operation(summary = "Ativar produto")
    @PostMapping("/{id}/ativar")
    public ResponseEntity<ProdutoDTO> ativar(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id) {
        try {
            log.info("Ativando produto: {}", id);
            ProdutoDTO produto = service.ativar(id);
            return ResponseEntity.ok(produto);
        } catch (IllegalArgumentException e) {
            log.warn("Produto não encontrado: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao ativar produto {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Inativar produto")
    @PostMapping("/{id}/inativar")
    public ResponseEntity<ProdutoDTO> inativar(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id) {
        try {
            log.info("Inativando produto: {}", id);
            ProdutoDTO produto = service.inativar(id);
            return ResponseEntity.ok(produto);
        } catch (IllegalArgumentException e) {
            log.warn("Produto não encontrado: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao inativar produto {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Atualizar estoque do produto")
    @PatchMapping("/{id}/estoque")
    public ResponseEntity<ProdutoDTO> atualizarEstoque(
            @Parameter(description = "ID do produto", required = true) @PathVariable String id,
            @Parameter(description = "Nova quantidade em estoque", required = true) @RequestParam Integer quantidade) {
        try {
            log.info("Atualizando estoque do produto {}: {}", id, quantidade);
            ProdutoDTO produto = service.atualizarEstoque(id, quantidade);
            return ResponseEntity.ok(produto);
        } catch (IllegalArgumentException e) {
            log.warn("Produto não encontrado: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao atualizar estoque do produto {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===========================
    // Query Operations
    // ===========================

    @Operation(summary = "Buscar produtos por categoria")
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ProdutoDTO>> buscarPorCategoria(
            @Parameter(description = "Categoria do produto", required = true) @PathVariable CategoriaProduto categoria) {
        try {
            log.debug("Buscando produtos por categoria: {}", categoria);
            List<ProdutoDTO> produtos = service.buscarPorCategoria(categoria);
            return produtos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(produtos);
        } catch (Exception e) {
            log.error("Erro ao buscar produtos por categoria {}", categoria, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Buscar produtos ativos")
    @GetMapping("/ativos")
    public ResponseEntity<List<ProdutoDTO>> buscarAtivos() {
        try {
            log.debug("Buscando produtos ativos");
            List<ProdutoDTO> produtos = service.buscarAtivos();
            return produtos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(produtos);
        } catch (Exception e) {
            log.error("Erro ao buscar produtos ativos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===========================
    // Pagination Operations
    // ===========================

    @Operation(summary = "Listar todos os produtos com paginação")
    @GetMapping(value = "/findAll", params = {"page", "size"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ProdutoDTO> findAll(
            @Parameter(description = "Número da página") @RequestParam("page") int page,
            @Parameter(description = "Tamanho da página") @RequestParam("size") int size) {
        try {
            log.debug("Buscando todos os produtos - página: {}, tamanho: {}", page, size);
            return service.buscarTodos(page, size);
        } catch (Exception e) {
            log.error("Erro ao buscar todos os produtos", e);
            throw e;
        }
    }

    @Operation(summary = "Listar todos os produtos com paginação e ordenação")
    @GetMapping(value = "/findAll", params = {"page", "size", "sort"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ProdutoDTO> findAll(
            @Parameter(description = "Número da página") @RequestParam("page") int page,
            @Parameter(description = "Tamanho da página") @RequestParam("size") int size,
            @Parameter(description = "Ordenação") @RequestParam("sort") String[] sort) {
        try {
            log.debug("Buscando todos os produtos com ordenação - página: {}, tamanho: {}", page, size);
            return service.buscarTodos(page, size, sort);
        } catch (Exception e) {
            log.error("Erro ao buscar todos os produtos com ordenação", e);
            throw e;
        }
    }
}
