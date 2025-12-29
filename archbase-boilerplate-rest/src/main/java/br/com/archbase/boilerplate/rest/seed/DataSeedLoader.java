package br.com.archbase.boilerplate.rest.seed;

import br.com.archbase.boilerplate.core.domain.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import br.com.archbase.boilerplate.core.application.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Loader de dados iniciais para desenvolvimento.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeedLoader implements CommandLineRunner {

    private final ProdutoService produtoService;

    @Override
    public void run(String... args) {
        log.info("Iniciando carga de dados de exemplo...");

        try {
            // Criar produtos de exemplo
            criarProdutoExemplo("Notebook Dell Inspiron", "Notebook Dell Intel Core i5, 8GB RAM, 256GB SSD",
                    new BigDecimal("3500.00"), 10, CategoriaProduto.ELETRONICOS, "NOTEBOOK-DELL-001");

            criarProdutoExemplo("Mouse Logitech Wireless", "Mouse sem fio Logitech com precisão avançada",
                    new BigDecimal("89.90"), 50, CategoriaProduto.ELETRONICOS, "MOUSE-LOGI-001");

            criarProdutoExemplo("Mesa Escritório", "Mesa de escritório em madeira maciça, 1.20m x 0.60m",
                    new BigDecimal("450.00"), 15, CategoriaProduto.MOVEIS, "MESA-ESC-001");

            criarProdutoExemplo("Cadeira Giratória", "Cadeira ergonômica com suporte lombar",
                    new BigDecimal("320.00"), 20, CategoriaProduto.MOVEIS, "CADEIRA-ERG-001");

            criarProdutoExemplo("Camiseta Algodão", "Camiseta 100% algodão, various cores disponíveis",
                    new BigDecimal("49.90"), 100, CategoriaProduto.vestuario, "CAMISETA-ALG-001");

            criarProdutoExemplo("Calça Jeans", "Calça jeans masculina, clássico, vários tamanhos",
                    new BigDecimal("119.90"), 80, CategoriaProduto.vestuario, "CALCA-JEANS-001");

            criarProdutoExemplo("Arroz Integral", "Pacote de arroz integral 1kg",
                    new BigDecimal("8.50"), 200, CategoriaProduto.ALIMENTOS, "ARROZ-INT-001");

            criarProdutoExemplo("Feijão Preto", "Pacote de feijão preto 1kg",
                    new BigDecimal("7.90"), 200, CategoriaProduto.ALIMENTOS, "FEIJAO-PRE-001");

            criarProdutoExemplo("Refrigerante Cola", "Refrigerante de cola 2L",
                    new BigDecimal("6.50"), 150, CategoriaProduto.BEBIDAS, "REFRI-COLA-001");

            criarProdutoExemplo("Água Mineral", "Água mineral garrafa 500ml",
                    new BigDecimal("2.50"), 300, CategoriaProduto.BEBIDAS, "AGUA-500-001");

            criarProdutoExemplo("Detergente Líquido", "Detergente líquido 500ml",
                    new BigDecimal("2.99"), 100, CategoriaProduto.LIMPEZA, "DET-LIQ-001");

            criarProdutoExemplo("Sabão em Pó", "Sabão em pó 1kg",
                    new BigDecimal("12.90"), 80, CategoriaProduto.LIMPEZA, "SAO-PO-001");

            criarProdutoExemplo("Shampoo Anticaspa", "Shampoo anticaspa 400ml",
                    new BigDecimal("18.90"), 60, CategoriaProduto.HIGIENE, "SHAMP-ANTI-001");

            criarProdutoExemplo("Creme Dental", "Creme dental 90g",
                    new BigDecimal("5.90"), 150, CategoriaProduto.HIGIENE, "CREME-DENT-001");

            log.info("Carga de dados concluída! 14 produtos de exemplo criados.");

        } catch (Exception e) {
            log.error("Erro durante carga de dados: {}", e.getMessage(), e);
        }
    }

    private void criarProdutoExemplo(String nome, String descricao, BigDecimal preco,
                                     Integer estoque, CategoriaProduto categoria, String sku) {
        try {
            ProdutoDTO dto = ProdutoDTO.builder()
                    .nome(nome)
                    .descricao(descricao)
                    .preco(preco)
                    .estoque(estoque)
                    .categoria(categoria)
                    .ativo(true)
                    .sku(sku)
                    .dataCadastro(LocalDateTime.now())
                    .build();

            produtoService.criar(dto);
            log.debug("Produto criado: {}", nome);

        } catch (Exception e) {
            log.warn("Não foi possível criar produto {}: {}", sku, e.getMessage());
        }
    }
}
