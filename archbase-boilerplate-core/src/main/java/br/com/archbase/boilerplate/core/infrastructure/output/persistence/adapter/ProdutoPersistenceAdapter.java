package br.com.archbase.boilerplate.core.infrastructure.output.persistence.adapter;

import br.com.archbase.boilerplate.core.application.port.out.ProdutoPersistencePort;
import br.com.archbase.boilerplate.core.domain.entity.Produto;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.ProdutoEntity;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.mapper.ProdutoPersistenceMapper;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository.ProdutoJpaRepository;
import br.com.archbase.query.rsql.jpa.SortUtils;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistência para produtos seguindo o padrão hexagonal.
 * <p>
 * Implementa {@link ProdutoPersistencePort} que estende {@link br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery}
 * para habilitar filtros dinâmicos via RSQL compatíveis com componentes de filtro do frontend.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProdutoPersistenceAdapter implements ProdutoPersistencePort {

    private final ProdutoJpaRepository repository;
    private final JPAQueryFactory queryFactory;
    private final ProdutoPersistenceMapper mapper;

    @Override
    public Produto save(Produto produto) {
        ProdutoEntity entity = mapper.toEntity(produto);
        ProdutoEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Produto> findBySku(String sku) {
        return repository.findBySku(sku)
                .map(mapper::toDomain);
    }

    @Override
    public List<Produto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Produto> findAll(List<String> ids) {
        List<ProdutoEntity> entities = repository.findAllById(ids);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Produto> findByCategoria(String categoria) {
        return repository.findByCategoria(
                br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto.valueOf(categoria)
        ).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Produto> findAtivos() {
        return repository.findByAtivoTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySku(String sku) {
        return repository.existsBySku(sku);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    // ============ MÉTODOS DE FindDataWithFilterQuery ============
    // Estes métodos habilitam filtros dinâmicos via RSQL para o frontend

    @Override
    public Produto findById(String id) {
        return repository.findById(id)
                .map(mapper::toDomain)
                .orElse(null);
    }

    @Override
    public Page<Produto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProdutoEntity> result = repository.findAll(pageable);
        List<Produto> list = result.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageProduto(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<Produto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ProdutoEntity> result = repository.findAll(pageable);
        List<Produto> list = result.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageProduto(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<Produto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProdutoEntity> result = repository.findAll(filter, pageable);
        List<Produto> list = result.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageProduto(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<Produto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ProdutoEntity> result = repository.findAll(filter, pageable);
        List<Produto> list = result.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageProduto(list, pageable, result.getTotalElements());
    }

    /**
     * Implementação de Page para Produto.
     */
    static class PageProduto extends PageImpl<Produto> {
        public PageProduto(List<Produto> content) {
            super(content);
        }

        public PageProduto(List<Produto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }
}
