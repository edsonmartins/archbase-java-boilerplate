package br.com.archbase.boilerplate.core.application.service;

import br.com.archbase.boilerplate.core.application.dto.ProdutoCreateDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoUpdateDTO;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import br.com.archbase.boilerplate.core.domain.exception.DuplicateEntityException;
import br.com.archbase.boilerplate.core.domain.exception.EntityNotFoundException;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity.ProdutoEntity;
import br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository.ProdutoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para ProdutoService.
 * Segue os padrões dos projetos vendax-promoter-api e gestor-rq-api.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProdutoService - Testes de Serviço de Produto")
class ProdutoServiceTest {

    @Mock
    private ProdutoJpaRepository repository;

    @InjectMocks
    private ProdutoService produtoService;

    private ProdutoEntity produtoEntity;
    private ProdutoCreateDTO createDTO;
    private ProdutoUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        produtoEntity = ProdutoEntity.builder()
                .id(UUID.randomUUID().toString())
                .nome("Produto Teste")
                .descricao("Descrição do produto de teste")
                .preco(new BigDecimal("99.90"))
                .estoque(100)
                .categoria(CategoriaProduto.ELETRONICOS)
                .ativo(true)
                .sku("SKU-TEST-001")
                .dataCadastro(LocalDateTime.now())
                .build();

        createDTO = ProdutoCreateDTO.builder()
                .nome("Novo Produto")
                .descricao("Descrição do novo produto")
                .preco(new BigDecimal("149.90"))
                .estoque(50)
                .categoria(CategoriaProduto.ROUPAS)
                .sku("SKU-NEW-001")
                .build();

        updateDTO = ProdutoUpdateDTO.builder()
                .nome("Produto Atualizado")
                .preco(new BigDecimal("199.90"))
                .build();
    }

    @Nested
    @DisplayName("Método criar")
    class CriarTests {

        @Test
        @DisplayName("Deve criar produto com dados válidos")
        void deveCriarProdutoComDadosValidos() {
            // Given
            when(repository.existsBySku(anyString())).thenReturn(false);
            when(repository.save(any(ProdutoEntity.class))).thenAnswer(invocation -> {
                ProdutoEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID().toString());
                return entity;
            });

            // When
            ProdutoDTO resultado = produtoService.criar(createDTO);

            // Then
            assertThat(resultado).isNotNull();
            assertThat(resultado.getNome()).isEqualTo(createDTO.getNome());
            assertThat(resultado.getPreco()).isEqualTo(createDTO.getPreco());
            verify(repository, times(1)).save(any(ProdutoEntity.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando SKU já existe")
        void deveLancarExcecaoQuandoSkuJaExiste() {
            // Given
            when(repository.existsBySku(createDTO.getSku())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> produtoService.criar(createDTO))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessageContaining("SKU");

            verify(repository, never()).save(any(ProdutoEntity.class));
        }

        @Test
        @DisplayName("Deve criar produto sem SKU")
        void deveCriarProdutoSemSku() {
            // Given
            createDTO.setSku(null);
            when(repository.save(any(ProdutoEntity.class))).thenAnswer(invocation -> {
                ProdutoEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID().toString());
                return entity;
            });

            // When
            ProdutoDTO resultado = produtoService.criar(createDTO);

            // Then
            assertThat(resultado).isNotNull();
            verify(repository, times(1)).save(any(ProdutoEntity.class));
        }
    }

    @Nested
    @DisplayName("Método atualizar")
    class AtualizarTests {

        @Test
        @DisplayName("Deve atualizar produto existente")
        void deveAtualizarProdutoExistente() {
            // Given
            String id = produtoEntity.getId();
            when(repository.findById(id)).thenReturn(Optional.of(produtoEntity));
            when(repository.save(any(ProdutoEntity.class))).thenReturn(produtoEntity);

            // When
            ProdutoDTO resultado = produtoService.atualizar(id, updateDTO);

            // Then
            assertThat(resultado).isNotNull();
            verify(repository, times(1)).save(any(ProdutoEntity.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando produto não encontrado")
        void deveLancarExcecaoQuandoProdutoNaoEncontrado() {
            // Given
            String id = "id-inexistente";
            when(repository.findById(id)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> produtoService.atualizar(id, updateDTO))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Produto");

            verify(repository, never()).save(any(ProdutoEntity.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando novo SKU já existe")
        void deveLancarExcecaoQuandoNovoSkuJaExiste() {
            // Given
            String id = produtoEntity.getId();
            updateDTO.setSku("SKU-EXISTENTE");
            when(repository.findById(id)).thenReturn(Optional.of(produtoEntity));
            when(repository.existsBySku("SKU-EXISTENTE")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> produtoService.atualizar(id, updateDTO))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessageContaining("SKU");

            verify(repository, never()).save(any(ProdutoEntity.class));
        }
    }

    @Nested
    @DisplayName("Método buscarPorId")
    class BuscarPorIdTests {

        @Test
        @DisplayName("Deve retornar produto quando encontrado")
        void deveRetornarProdutoQuandoEncontrado() {
            // Given
            String id = produtoEntity.getId();
            when(repository.findById(id)).thenReturn(Optional.of(produtoEntity));

            // When
            ProdutoDTO resultado = produtoService.buscarPorId(id);

            // Then
            assertThat(resultado).isNotNull();
            assertThat(resultado.getNome()).isEqualTo(produtoEntity.getNome());
        }

        @Test
        @DisplayName("Deve retornar null quando não encontrado")
        void deveRetornarNullQuandoNaoEncontrado() {
            // Given
            String id = "id-inexistente";
            when(repository.findById(id)).thenReturn(Optional.empty());

            // When
            ProdutoDTO resultado = produtoService.buscarPorId(id);

            // Then
            assertThat(resultado).isNull();
        }
    }

    @Nested
    @DisplayName("Método remover")
    class RemoverTests {

        @Test
        @DisplayName("Deve remover produto existente")
        void deveRemoverProdutoExistente() {
            // Given
            String id = produtoEntity.getId();
            when(repository.existsById(id)).thenReturn(true);
            doNothing().when(repository).deleteById(id);

            // When
            produtoService.remover(id);

            // Then
            verify(repository, times(1)).deleteById(id);
        }

        @Test
        @DisplayName("Deve lançar exceção quando produto não existe")
        void deveLancarExcecaoQuandoProdutoNaoExiste() {
            // Given
            String id = "id-inexistente";
            when(repository.existsById(id)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> produtoService.remover(id))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(repository, never()).deleteById(anyString());
        }
    }

    @Nested
    @DisplayName("Métodos de status")
    class StatusTests {

        @Test
        @DisplayName("Deve ativar produto")
        void deveAtivarProduto() {
            // Given
            String id = produtoEntity.getId();
            produtoEntity.setAtivo(false);
            when(repository.findById(id)).thenReturn(Optional.of(produtoEntity));
            when(repository.save(any(ProdutoEntity.class))).thenReturn(produtoEntity);

            // When
            ProdutoDTO resultado = produtoService.ativar(id);

            // Then
            assertThat(resultado).isNotNull();
            verify(repository, times(1)).save(any(ProdutoEntity.class));
        }

        @Test
        @DisplayName("Deve inativar produto")
        void deveInativarProduto() {
            // Given
            String id = produtoEntity.getId();
            when(repository.findById(id)).thenReturn(Optional.of(produtoEntity));
            when(repository.save(any(ProdutoEntity.class))).thenReturn(produtoEntity);

            // When
            ProdutoDTO resultado = produtoService.inativar(id);

            // Then
            assertThat(resultado).isNotNull();
            verify(repository, times(1)).save(any(ProdutoEntity.class));
        }
    }

    @Nested
    @DisplayName("Métodos de busca")
    class BuscaTests {

        @Test
        @DisplayName("Deve buscar produtos por categoria")
        void deveBuscarProdutosPorCategoria() {
            // Given
            when(repository.findByCategoria(CategoriaProduto.ELETRONICOS))
                    .thenReturn(List.of(produtoEntity));

            // When
            List<ProdutoDTO> resultado = produtoService.buscarPorCategoria(CategoriaProduto.ELETRONICOS);

            // Then
            assertThat(resultado).isNotEmpty();
            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar produtos ativos")
        void deveBuscarProdutosAtivos() {
            // Given
            when(repository.findByAtivoTrue()).thenReturn(List.of(produtoEntity));

            // When
            List<ProdutoDTO> resultado = produtoService.buscarAtivos();

            // Then
            assertThat(resultado).isNotEmpty();
            assertThat(resultado).hasSize(1);
        }
    }
}
