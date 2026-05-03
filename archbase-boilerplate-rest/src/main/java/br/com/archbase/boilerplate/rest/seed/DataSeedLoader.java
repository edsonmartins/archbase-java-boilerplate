package br.com.archbase.boilerplate.rest.seed;

import br.com.archbase.boilerplate.core.application.service.ProdutoService;
import br.com.archbase.boilerplate.core.application.dto.ProdutoCreateDTO;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Loader de dados iniciais para desenvolvimento.
 * Segue os padrões dos projetos vendax-promoter-api e gestor-rq-api.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
@Order(100)
public class DataSeedLoader implements CommandLineRunner {

    private final ProdutoService produtoService;

    @Override
    public void run(String... args) {
        log.info("=== Iniciando carga de dados de exemplo ===");

        try {
            int count = 0;

            // Eletrônicos
            count += criarProdutoExemplo("Notebook Dell Inspiron", "Notebook Dell Intel Core i5, 8GB RAM, 256GB SSD",
                    new BigDecimal("3500.00"), 10, CategoriaProduto.ELETRONICOS, "NOTEBOOK-DELL-001", "Dell");

            count += criarProdutoExemplo("Mouse Logitech Wireless", "Mouse sem fio Logitech com precisão avançada",
                    new BigDecimal("89.90"), 50, CategoriaProduto.ELETRONICOS, "MOUSE-LOGI-001", "Logitech");

            count += criarProdutoExemplo("Teclado Mecânico RGB", "Teclado mecânico com iluminação RGB e switches azuis",
                    new BigDecimal("299.90"), 30, CategoriaProduto.ELETRONICOS, "TECLADO-MECA-001", "HyperX");

            count += criarProdutoExemplo("Monitor 27\" 4K", "Monitor LED 27 polegadas com resolução 4K UHD",
                    new BigDecimal("2499.90"), 15, CategoriaProduto.ELETRONICOS, "MONITOR-27-4K-001", "Samsung");

            // Móveis
            count += criarProdutoExemplo("Mesa Escritório", "Mesa de escritório em madeira maciça, 1.20m x 0.60m",
                    new BigDecimal("450.00"), 15, CategoriaProduto.MOVEIS, "MESA-ESC-001", "Tok&Stok");

            count += criarProdutoExemplo("Cadeira Giratória", "Cadeira ergonômica com suporte lombar",
                    new BigDecimal("320.00"), 20, CategoriaProduto.MOVEIS, "CADEIRA-ERG-001", "Flexform");

            // Vestuário
            count += criarProdutoExemplo("Camiseta Algodão", "Camiseta 100% algodão, várias cores disponíveis",
                    new BigDecimal("49.90"), 100, CategoriaProduto.ROUPAS, "CAMISETA-ALG-001", "Hering");

            count += criarProdutoExemplo("Calça Jeans", "Calça jeans masculina, clássico, vários tamanhos",
                    new BigDecimal("119.90"), 80, CategoriaProduto.ROUPAS, "CALCA-JEANS-001", "Levi's");

            // Alimentos
            count += criarProdutoExemplo("Arroz Integral", "Pacote de arroz integral 1kg",
                    new BigDecimal("8.50"), 200, CategoriaProduto.ALIMENTOS, "ARROZ-INT-001", "Camil");

            count += criarProdutoExemplo("Feijão Preto", "Pacote de feijão preto 1kg",
                    new BigDecimal("7.90"), 200, CategoriaProduto.ALIMENTOS, "FEIJAO-PRE-001", "Camil");

            // Bebidas
            count += criarProdutoExemplo("Refrigerante Cola", "Refrigerante de cola 2L",
                    new BigDecimal("6.50"), 150, CategoriaProduto.BEBIDAS, "REFRI-COLA-001", "Coca-Cola");

            count += criarProdutoExemplo("Água Mineral", "Água mineral garrafa 500ml",
                    new BigDecimal("2.50"), 300, CategoriaProduto.BEBIDAS, "AGUA-500-001", "Crystal");

            // Limpeza
            count += criarProdutoExemplo("Detergente Líquido", "Detergente líquido 500ml",
                    new BigDecimal("2.99"), 100, CategoriaProduto.LIMPEZA, "DET-LIQ-001", "Ypê");

            count += criarProdutoExemplo("Sabão em Pó", "Sabão em pó 1kg",
                    new BigDecimal("12.90"), 80, CategoriaProduto.LIMPEZA, "SABAO-PO-001", "OMO");

            // Higiene
            count += criarProdutoExemplo("Shampoo Anticaspa", "Shampoo anticaspa 400ml",
                    new BigDecimal("18.90"), 60, CategoriaProduto.HIGIENE, "SHAMP-ANTI-001", "Head & Shoulders");

            count += criarProdutoExemplo("Creme Dental", "Creme dental 90g",
                    new BigDecimal("5.90"), 150, CategoriaProduto.HIGIENE, "CREME-DENT-001", "Colgate");

            log.info("=== Carga de dados concluída! {} produtos criados ===", count);

        } catch (Exception e) {
            log.error("Erro durante carga de dados: {}", e.getMessage(), e);
        }
    }

    private int criarProdutoExemplo(String nome, String descricao, BigDecimal preco,
                                     Integer estoque, CategoriaProduto categoria, String sku, String marca) {
        try {
            ProdutoCreateDTO dto = ProdutoCreateDTO.builder()
                    .nome(nome)
                    .descricao(descricao)
                    .preco(preco)
                    .estoque(estoque)
                    .categoria(categoria)
                    .sku(sku)
                    .marca(marca)
                    .build();

            produtoService.criar(dto);
            log.debug("Produto criado: {} (SKU: {})", nome, sku);
            return 1;

        } catch (Exception e) {
            log.warn("Produto {} já existe ou erro ao criar: {}", sku, e.getMessage());
            return 0;
        }
    }
}
