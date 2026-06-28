package br.com.archbase.boilerplate.core.infrastructure.output.persistence.adapter;

import br.com.archbase.boilerplate.core.application.dto.ProdutoEstatisticasDTO;
import br.com.archbase.boilerplate.core.application.port.out.ProdutoPersistencePort;
import br.com.archbase.boilerplate.core.domain.entity.Produto;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.ProdutoEntity;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.QProdutoEntity;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.mapper.ProdutoPersistenceMapper;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository.ProdutoJpaRepository;
import br.com.archbase.query.rsql.jpa.SortUtils;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final QProdutoEntity qProduto = QProdutoEntity.produtoEntity;

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
    @Transactional(readOnly = true)
    public Optional<Produto> findBySku(String sku) {
        return repository.findBySku(sku)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> findAll(List<String> ids) {
        List<ProdutoEntity> entities = repository.findAllById(ids);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> findByCategoria(String categoria) {
        return repository.findByCategoria(CategoriaProduto.valueOf(categoria)).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> findAtivos() {
        return repository.findByAtivoTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySku(String sku) {
        return repository.existsBySku(sku);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProdutoEstatisticasDTO obterEstatisticas() {
        Long totalProdutos = queryFactory
                .select(qProduto.count())
                .from(qProduto)
                .fetchOne();

        Long produtosAtivos = queryFactory
                .select(qProduto.count())
                .from(qProduto)
                .where(qProduto.ativo.isTrue())
                .fetchOne();

        Long produtosInativos = queryFactory
                .select(qProduto.count())
                .from(qProduto)
                .where(qProduto.ativo.isFalse())
                .fetchOne();

        Long produtosEmDestaque = queryFactory
                .select(qProduto.count())
                .from(qProduto)
                .where(qProduto.destaque.isTrue())
                .fetchOne();

        Double precoMedioDouble = queryFactory
                .select(qProduto.preco.avg())
                .from(qProduto)
                .fetchOne();
        BigDecimal precoMedio = precoMedioDouble != null ? BigDecimal.valueOf(precoMedioDouble) : null;

        BigDecimal precoMinimo = queryFactory
                .select(qProduto.preco.min())
                .from(qProduto)
                .fetchOne();

        BigDecimal precoMaximo = queryFactory
                .select(qProduto.preco.max())
                .from(qProduto)
                .fetchOne();

        Long estoqueTotal = queryFactory
                .select(qProduto.estoque.sumAggregate().longValue())
                .from(qProduto)
                .fetchOne();

        Map<String, Long> produtosPorCategoria = new HashMap<>();
        List<Tuple> categoriaResults = queryFactory
                .select(qProduto.categoria, qProduto.count())
                .from(qProduto)
                .groupBy(qProduto.categoria)
                .fetch();
        for (Tuple tuple : categoriaResults) {
            CategoriaProduto cat = tuple.get(qProduto.categoria);
            Long count = tuple.get(qProduto.count());
            if (cat != null && count != null) {
                produtosPorCategoria.put(cat.name(), count);
            }
        }

        Map<String, Long> produtosPorMarca = new HashMap<>();
        List<Tuple> marcaResults = queryFactory
                .select(qProduto.marca, qProduto.count())
                .from(qProduto)
                .where(qProduto.marca.isNotNull())
                .groupBy(qProduto.marca)
                .fetch();
        for (Tuple tuple : marcaResults) {
            String marca = tuple.get(qProduto.marca);
            Long count = tuple.get(qProduto.count());
            if (marca != null && count != null) {
                produtosPorMarca.put(marca, count);
            }
        }

        return ProdutoEstatisticasDTO.builder()
                .totalProdutos(totalProdutos != null ? totalProdutos : 0L)
                .produtosAtivos(produtosAtivos != null ? produtosAtivos : 0L)
                .produtosInativos(produtosInativos != null ? produtosInativos : 0L)
                .produtosEmDestaque(produtosEmDestaque != null ? produtosEmDestaque : 0L)
                .precoMedio(precoMedio)
                .precoMinimo(precoMinimo)
                .precoMaximo(precoMaximo)
                .estoqueTotal(estoqueTotal != null ? estoqueTotal : 0L)
                .produtosPorCategoria(produtosPorCategoria)
                .produtosPorMarca(produtosPorMarca)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> findByPrecoEntre(BigDecimal min, BigDecimal max) {
        List<ProdutoEntity> entities = queryFactory
                .selectFrom(qProduto)
                .where(qProduto.preco.between(min, max))
                .fetch();
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> findEmDestaque() {
        List<ProdutoEntity> entities = queryFactory
                .selectFrom(qProduto)
                .where(qProduto.destaque.isTrue())
                .fetch();
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> findByMarca(String marca) {
        List<ProdutoEntity> entities = queryFactory
                .selectFrom(qProduto)
                .where(qProduto.marca.equalsIgnoreCase(marca))
                .fetch();
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    // ============ MÉTODOS DE FindDataWithFilterQuery ============
    // Estes métodos habilitam filtros dinâmicos via RSQL para o frontend

    @Override
    @Transactional(readOnly = true)
    public Produto findById(String id) {
        return repository.findById(id)
                .map(mapper::toDomain)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Produto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProdutoEntity> result = repository.findAll(pageable);
        List<Produto> list = result.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageProduto(list, pageable, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Produto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ProdutoEntity> result = repository.findAll(pageable);
        List<Produto> list = result.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageProduto(list, pageable, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Produto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProdutoEntity> result = repository.findAll(filter, pageable);
        List<Produto> list = result.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageProduto(list, pageable, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
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
