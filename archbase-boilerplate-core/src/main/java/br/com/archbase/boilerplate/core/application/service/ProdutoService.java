package br.com.archbase.boilerplate.core.application.service;

import br.com.archbase.boilerplate.core.application.dto.ProdutoCreateDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoUpdateDTO;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import br.com.archbase.boilerplate.core.domain.exception.DuplicateEntityException;
import br.com.archbase.boilerplate.core.domain.exception.EntityNotFoundException;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.ProdutoEntity;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository.ProdutoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de Produto contendo regras de negócio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProdutoService {

    private final ProdutoJpaRepository repository;

    /**
     * Cria um novo produto a partir de ProdutoCreateDTO.
     */
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
        log.info("Produto criado com ID: {}", saved.getId());

        return saved.toDTO();
    }

    /**
     * Cria um novo produto a partir de ProdutoDTO (mantido para compatibilidade).
     */
    @Transactional
    public ProdutoDTO criar(ProdutoDTO dto) {
        log.info("Criando produto: {}", dto.getNome());

        // Verificar SKU único
        if (dto.getSku() != null && repository.existsBySku(dto.getSku())) {
            throw new DuplicateEntityException("Produto", "SKU", dto.getSku());
        }

        ProdutoEntity entity = ProdutoEntity.fromDTO(dto);
        entity.setDataCadastro(LocalDateTime.now());

        ProdutoEntity saved = repository.save(entity);
        log.info("Produto criado com ID: {}", saved.getId());

        return saved.toDTO();
    }

    /**
     * Atualiza um produto existente com ProdutoUpdateDTO.
     */
    @Transactional
    public ProdutoDTO atualizar(String id, ProdutoUpdateDTO dto) {
        log.info("Atualizando produto: id={}", id);

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
        if (dto.getDescricao() != null) entity.setDescricao(dto.getDescricao());
        if (dto.getPreco() != null) entity.setPreco(dto.getPreco());
        if (dto.getEstoque() != null) entity.setEstoque(dto.getEstoque());
        if (dto.getCategoria() != null) entity.setCategoria(dto.getCategoria());
        if (dto.getAtivo() != null) entity.setAtivo(dto.getAtivo());
        if (dto.getMarca() != null) entity.setMarca(dto.getMarca());
        if (dto.getUrlImagem() != null) entity.setUrlImagem(dto.getUrlImagem());
        if (dto.getDestaque() != null) entity.setDestaque(dto.getDestaque());

        ProdutoEntity saved = repository.save(entity);
        log.info("Produto atualizado: {}", saved.getId());

        return saved.toDTO();
    }

    /**
     * Atualiza um produto existente com ProdutoDTO (mantido para compatibilidade).
     */
    @Transactional
    public ProdutoDTO atualizar(String id, ProdutoDTO dto) {
        log.info("Atualizando produto: id={}", id);

        ProdutoEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto", id));

        // Verificar SKU se foi alterado
        if (dto.getSku() != null && !dto.getSku().equals(entity.getSku())) {
            if (repository.existsBySku(dto.getSku())) {
                throw new DuplicateEntityException("Produto", "SKU", dto.getSku());
            }
        }

        entity.setNome(dto.getNome());
        entity.setDescricao(dto.getDescricao());
        entity.setPreco(dto.getPreco());
        entity.setEstoque(dto.getEstoque());
        entity.setCategoria(dto.getCategoria());
        entity.setAtivo(dto.getAtivo());
        entity.setSku(dto.getSku());

        ProdutoEntity saved = repository.save(entity);
        log.info("Produto atualizado: {}", saved.getId());

        return saved.toDTO();
    }

    /**
     * Busca produto por ID.
     */
    public ProdutoDTO buscarPorId(String id) {
        log.debug("Buscando produto por ID: {}", id);

        return repository.findById(id)
                .map(ProdutoEntity::toDTO)
                .orElse(null);
    }

    /**
     * Busca produto por SKU.
     */
    public ProdutoDTO buscarPorSku(String sku) {
        log.debug("Buscando produto por SKU: {}", sku);

        return repository.findBySku(sku)
                .map(ProdutoEntity::toDTO)
                .orElse(null);
    }

    /**
     * Lista todos os produtos com paginação.
     */
    public Page<ProdutoDTO> buscarTodos(int page, int size) {
        log.debug("Listando produtos - página: {}, tamanho: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable)
                .map(ProdutoEntity::toDTO);
    }

    /**
     * Lista todos os produtos com paginação e ordenação.
     */
    public Page<ProdutoDTO> buscarTodos(int page, int size, String[] sort) {
        log.debug("Listando produtos com ordenação - página: {}, tamanho: {}", page, size);

        Sort sortObj = Sort.by(sort != null && sort.length > 0 ? sort[0] : "nome");
        if (sort != null && sort.length > 1 && sort[1].equalsIgnoreCase("desc")) {
            sortObj = sortObj.descending();
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        return repository.findAll(pageable)
                .map(ProdutoEntity::toDTO);
    }

    /**
     * Lista produtos por categoria.
     */
    public List<ProdutoDTO> buscarPorCategoria(CategoriaProduto categoria) {
        log.debug("Listando produtos por categoria: {}", categoria);

        return repository.findByCategoria(categoria).stream()
                .map(ProdutoEntity::toDTO)
                .toList();
    }

    /**
     * Lista produtos ativos.
     */
    public List<ProdutoDTO> buscarAtivos() {
        log.debug("Listando produtos ativos");

        return repository.findByAtivoTrue().stream()
                .map(ProdutoEntity::toDTO)
                .toList();
    }

    /**
     * Remove um produto.
     */
    @Transactional
    public void remover(String id) {
        log.info("Removendo produto: {}", id);

        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Produto", id);
        }

        repository.deleteById(id);
        log.info("Produto removido: {}", id);
    }

    /**
     * Atualiza estoque do produto.
     */
    @Transactional
    public ProdutoDTO atualizarEstoque(String id, Integer quantidade) {
        log.info("Atualizando estoque do produto: {}, quantidade: {}", id, quantidade);

        ProdutoEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto", id));

        entity.setEstoque(quantidade);
        ProdutoEntity saved = repository.save(entity);

        return saved.toDTO();
    }

    /**
     * Ativa produto.
     */
    @Transactional
    public ProdutoDTO ativar(String id) {
        log.info("Ativando produto: {}", id);

        ProdutoEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto", id));

        entity.setAtivo(true);
        return repository.save(entity).toDTO();
    }

    /**
     * Inativa produto.
     */
    @Transactional
    public ProdutoDTO inativar(String id) {
        log.info("Inativando produto: {}", id);

        ProdutoEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto", id));

        entity.setAtivo(false);
        return repository.save(entity).toDTO();
    }
}
