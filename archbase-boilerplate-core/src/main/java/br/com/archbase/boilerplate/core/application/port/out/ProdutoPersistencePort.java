package br.com.archbase.boilerplate.core.application.port.out;

import br.com.archbase.boilerplate.core.application.dto.ProdutoEstatisticasDTO;
import br.com.archbase.boilerplate.core.domain.entity.Produto;
import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para operações de persistência de produtos.
 * <p>
 * Seguindo o padrão hexagonal (Ports and Adapters).
 * Estende {@link FindDataWithFilterQuery} para habilitar filtros dinâmicos via RSQL
 * compatíveis com componentes de filtro do frontend.
 * </p>
 * <p>
 * NOTA: FindDataWithFilterQuery já fornece:
 * - T findById(String id) - retorna null se não encontrado
 * - Page<T> findAll(int page, int size)
 * - Page<T> findAll(int page, int size, String[] sort)
 * - List<T> findAll(List<String> ids)
 * - Page<T> findWithFilter(String filter, int page, int size)
 * - Page<T> findWithFilter(String filter, int page, int size, String[] sort)
 * </p>
 */
public interface ProdutoPersistencePort extends FindDataWithFilterQuery<String, Produto> {

    /**
     * Salva um produto (cria ou atualiza).
     */
    Produto save(Produto produto);

    /**
     * Busca produto por SKU.
     */
    Optional<Produto> findBySku(String sku);

    /**
     * Lista todos os produtos.
     */
    List<Produto> findAll();

    /**
     * Lista produtos por categoria.
     */
    List<Produto> findByCategoria(String categoria);

    /**
     * Lista produtos ativos.
     */
    List<Produto> findAtivos();

    /**
     * Verifica se SKU já existe.
     */
    boolean existsBySku(String sku);

    /**
     * Remove produto por ID.
     */
    void deleteById(String id);

    /**
     * Obtém estatísticas agregadas dos produtos.
     */
    ProdutoEstatisticasDTO obterEstatisticas();

    /**
     * Busca produtos por faixa de preço.
     */
    List<Produto> findByPrecoEntre(BigDecimal min, BigDecimal max);

    /**
     * Busca produtos em destaque.
     */
    List<Produto> findEmDestaque();

    /**
     * Busca produtos por marca.
     */
    List<Produto> findByMarca(String marca);
}
