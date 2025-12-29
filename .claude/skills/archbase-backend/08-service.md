# 08. Services (Camada de Aplicação)

Services implementam os Use Cases (Ports de Entrada).

---

## Conceito

**CRÍTICO**: Service deve:
- Implementar um Port de entrada (Use Case)
- Injetar Ports de saída (Persistence, External)
- Conter lógica de negócio orquestrada
- Usar @Service
- Não depender diretamente de Entities JPA

---

## Service Básico

```java
package br.com.empresa.projeto.core.application.service;

import br.com.archbase.multitenancy.ArchbaseCurrentTenantIdentifierResolver;
import br.com.empresa.projeto.core.application.dto.CreateExampleDTO;
import br.com.empresa.projeto.core.application.dto.ExampleDTO;
import br.com.empresa.projeto.core.application.dto.UpdateExampleDTO;
import br.com.empresa.projeto.core.application.mapper.ExampleMapper;
import br.com.empresa.projeto.core.application.port.out.ExamplePersistencePort;
import br.com.empresa.projeto.core.application.port.in.CreateExampleUseCase;
import br.com.empresa.projeto.core.application.port.in.UpdateExampleUseCase;
import br.com.empresa.projeto.core.application.port.in.DeleteExampleUseCase;
import br.com.empresa.projeto.core.application.port.in.FindExampleUseCase;
import br.com.empresa.projeto.core.domain.entity.Example;
import br.com.empresa.projeto.core.domain.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleService implements
        CreateExampleUseCase,
        UpdateExampleUseCase,
        DeleteExampleUseCase,
        FindExampleUseCase {

    private final ExamplePersistencePort persistencePort;
    private final ExampleMapper mapper;
    private final ArchbaseCurrentTenantIdentifierResolver tenantResolver;

    @Override
    @Transactional
    public ExampleDTO create(CreateExampleDTO dto) {
        log.info("Criando example: {}", dto.getNome());

        String tenantId = tenantResolver.getCurrentTenantId();

        // Validação de negócio
        if (persistencePort.existsByNome(dto.getNome(), tenantId)) {
            throw new DomainException("Já existe example com este nome");
        }

        // Criar domain object
        Example example = new Example();
        example.setId(generateId());
        example.setNome(dto.getNome());
        example.setDescricao(dto.getDescricao());
        example.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        // Salvar
        Example saved = persistencePort.save(example);

        log.info("Example criado: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public ExampleDTO update(String id, UpdateExampleDTO dto) {
        log.info("Atualizando example: {}", id);

        Example example = persistencePort.findById(id)
                .orElseThrow(() -> new DomainException("Example não encontrado: " + id));

        // Atualiza campos fornecidos
        dto.getNome().ifPresent(example::setNome);
        dto.getDescricao().ifPresent(example::setDescricao);
        dto.getAtivo().ifPresent(example::setAtivo);

        Example saved = persistencePort.save(example);

        log.info("Example atualizado: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Deletando example: {}", id);

        if (!persistencePort.existsById(id)) {
            throw new DomainException("Example não encontrado: " + id);
        }

        persistencePort.deleteById(id);
        log.info("Example deletado: {}", id);
    }

    @Override
    public ExampleDTO findById(String id) {
        return persistencePort.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new DomainException("Example não encontrado: " + id));
    }

    @Override
    public PageResponseDTO<ExampleDTO> findAll(int page, int size) {
        String tenantId = tenantResolver.getCurrentTenantId();
        Page<Example> result = persistencePort.findAll(tenantId, PageRequest.of(page, size));
        return PageResponseDTO.from(result.map(mapper::toDTO));
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
```

---

## Service com Validação de Negócio

```java
@Service
@RequiredArgsConstructor
public class PedidoService implements CreatePedidoUseCase {

    private final PedidoPersistencePort pedidoPort;
    private final ClientePersistencePort clientePort;
    private final ProdutoPersistencePort produtoPort;
    private final EstoqueService estoqueService;
    private final PedidoMapper mapper;

    @Override
    @Transactional
    public PedidoDTO criar(CreatePedidoDTO dto) {
        log.info("Criando pedido para cliente: {}", dto.getClienteId());

        // 1. Buscar e validar cliente
        Cliente cliente = clientePort.findById(dto.getClienteId())
                .orElseThrow(() -> new DomainException("Cliente não encontrado"));

        if (!cliente.isAtivo()) {
            throw new DomainException("Cliente não está ativo");
        }

        // 2. Validar itens
        if (dto.getItens() == null || dto.getItens().isEmpty()) {
            throw new DomainException("Pedido deve ter pelo menos 1 item");
        }

        // 3. Criar pedido
        Pedido pedido = new Pedido();
        pedido.setId(generateId());
        pedido.setClienteId(cliente.getId());
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setStatus(Pedido.Status.CRIADO);

        // 4. Processar itens
        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateItemPedidoDTO itemDto : dto.getItens()) {
            Produto produto = produtoPort.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new DomainException("Produto não encontrado"));

            if (!produto.isAtivo()) {
                throw new DomainException("Produto " + produto.getNome() + " não está ativo");
            }

            // Verificar estoque
            if (!estoqueService.temEstoque(produto.getId(), itemDto.getQuantidade())) {
                throw new DomainException("Produto " + produto.getNome() + " sem estoque suficiente");
            }

            ItemPedido item = new ItemPedido();
            item.setProdutoId(produto.getId());
            item.setProdutoNome(produto.getNome());
            item.setQuantidade(itemDto.getQuantidade());
            item.setValorUnitario(produto.getPreco());
            item.setValorTotal(itemDto.getQuantidade().multiply(produto.getPreco()));

            itens.add(item);
            total = total.add(item.getValorTotal());
        }

        pedido.setItens(itens);
        pedido.setValorTotal(total);

        // 5. Reservar estoque
        estoqueService.reservarEstoque(pedido);

        // 6. Salvar
        Pedido saved = pedidoPort.save(pedido);

        // 7. Notificar
        notificationService.notifyPedidoCriado(saved);

        log.info("Pedido criado: {}", saved.getId());
        return mapper.toDTO(saved);
    }
}
```

---

## Service com Múltiplos Ports

```java
@Service
@RequiredArgsConstructor
public class ProdutoService {

    // Ports de persistência
    private final ProdutoPersistencePort produtoPort;
    private final CategoriaPersistencePort categoriaPort;

    // Ports externos
    private final IntegracaoERPAdapter erpAdapter;
    private final NotificacaoPort notificacaoPort;

    // Mapper
    private final ProdutoMapper mapper;

    @Transactional
    public ProdutoDTO criar(CreateProdutoDTO dto) {
        // 1. Validar categoria
        if (!categoriaPort.existsById(dto.getCategoriaId())) {
            throw new DomainException("Categoria não encontrada");
        }

        // 2. Criar produto
        Produto produto = mapper.toDomain(dto);
        produto.setId(generateSKU(dto));

        // 3. Salvar localmente
        Produto saved = produtoPort.save(produto);

        // 4. Sincronizar com ERP
        try {
            erpAdapter.enviarProduto(saved);
        } catch (Exception e) {
            log.error("Erro ao sincronizar com ERP", e);
            // Não falha a operação principal
        }

        // 5. Notificar
        notificacaoPort.notifyProdutoCriado(saved);

        return mapper.toDTO(saved);
    }
}
```

---

## Service para Busca com Filtros

```java
@Service
@RequiredArgsConstructor
public class QueryService {

    private final ExamplePersistencePort persistencePort;
    private final ExampleMapper mapper;
    private final ArchbaseCurrentTenantIdentifierResolver tenantResolver;

    public PageResponseDTO<ExampleDTO> buscar(ExampleFilterDTO filter, int page, int size) {
        String tenantId = tenantResolver.getCurrentTenantId();
        filter.setTenantId(tenantId);

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by("nome").ascending()
        );

        Page<Example> result = persistencePort.findByFilter(filter, pageable);
        return PageResponseDTO.from(result.map(mapper::toDTO));
    }

    public List<ExampleDTO> buscarAtivos() {
        String tenantId = tenantResolver.getCurrentTenantId();
        return persistencePort.findByAtivoAndTenantId(true, tenantId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
}
```

---

## Service com Orquestração Complexa

```java
@Service
@RequiredArgsConstructor
public class PedidoWorkflowService {

    private final PedidoPersistencePort pedidoPort;
    private final PagamentoService pagamentoService;
    private final EstoqueService estoqueService;
    private final EmailService emailService;
    private final IntegracaoLogisticaAdapter logisticaAdapter;

    @Transactional
    public void processarPedidoPagado(String pedidoId) {
        // 1. Buscar pedido
        Pedido pedido = pedidoPort.findById(pedidoId)
                .orElseThrow(() -> new DomainException("Pedido não encontrado"));

        // 2. Validar status
        if (pedido.getStatus() != Pedido.Status.PENDENTE_PAGAMENTO) {
            throw new DomainException("Pedido não está pendente de pagamento");
        }

        // 3. Confirmar pagamento
        pedido.pagar();
        pedidoPort.save(pedido);

        // 4. Reservar estoque
        estoqueService.reservarItens(pedido);

        // 5. Enviar para logística
        logisticaAdapter.criarRemessa(pedido);

        // 6. Enviar email
        emailService.enviarConfirmacaoPedido(pedido);

        // 7. Atualizar status
        pedido.enviar();
        pedidoPort.save(pedido);
    }
}
```

---

## Service Helpers

```java
@Service
@RequiredArgsConstructor
public class TenantService {

    private final ArchbaseCurrentTenantIdentifierResolver tenantResolver;

    protected String getCurrentTenantId() {
        return tenantResolver.getCurrentTenantId();
    }

    protected void validateTenant(String tenantId) {
        String current = getCurrentTenantId();
        if (!current.equals(tenantId)) {
            throw new DomainException("Acesso negado: tenant inválido");
        }
    }
}
```

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **@Transactional** | Em métodos que escrevem dados |
| **Implementar Use Cases** | Services implementam ports de entrada |
| **Injetar Ports OUT** | Services injetam persistence/external ports |
| **Domain Objects** | Trabalhar com Domain, não Entities |
| **Validações** | Lançar DomainException para regras de negócio |
| **Log** | Logar operações importantes |
| **@RequiredArgsConstructor** | Injeção via construtor |

---

**IMPORTANTE**: Services orquestram casos de uso. Lógica de domínio específica fica no Domain Object.
