# 06. Ports (Arquitetura Hexagonal)

Ports definem os contratos entre a camada de aplicação e a infraestrutura.

---

## Conceito

**CRÍTICO**: Ports são interfaces que:
- Definem contratos (what, não how)
- Separam lógica de negócio de infraestrutura
- Permitem trocar implementações
- São de dois tipos: Entrada (Use Cases) e Saída (Persistence, External)

---

## Estrutura de Pacotes

```
application/port/
├── in/                             # Ports de entrada (Use Cases)
│   ├── CreateExampleUseCase.java
│   ├── UpdateExampleUseCase.java
│   └── DeleteExampleUseCase.java
└── out/                            # Ports de saída (Adapters)
    ├── ExamplePersistencePort.java
    ├── ExampleNotificationPort.java
    └── ExampleExternalServicePort.java
```

---

## Port de Saída (Persistence)

```java
package br.com.empresa.projeto.core.application.port.out;

import br.com.empresa.projeto.core.domain.entity.Example;
import java.util.List;
import java.util.Optional;

public interface ExamplePersistencePort {

    Example save(Example example);

    Optional<Example> findById(String id);

    List<Example> findAll();

    List<Example> findByTenantId(String tenantId);

    List<Example> findByNomeContaining(String nome, String tenantId);

    void deleteById(String id);

    boolean existsById(String id);
}
```

---

## Port de Saída com Filtros

```java
package br.com.empresa.projeto.core.application.port.out;

import br.com.empresa.projeto.core.application.dto.ExampleFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExamplePersistencePort {

    Page<Example> findAll(Pageable pageable);

    Page<Example> findByFilter(ExampleFilterDTO filter, Pageable pageable);

    List<Example> findByAtivoAndTenantId(Boolean ativo, String tenantId);
}
```

---

## DTO de Filtro

```java
package br.com.empresa.projeto.core.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Optional;

@Schema(description = "Filtro de busca de Example")
public class ExampleFilterDTO {

    @Schema(description = "Nome para busca parcial")
    private Optional<String> nome;

    @Schema(description = "Status ativo")
    private Optional<Boolean> ativo;

    @Schema(description = "Data início criação")
    private Optional<LocalDateTime> dataCriacaoInicio;

    @Schema(description = "Data fim criação")
    private Optional<LocalDateTime> dataCriacaoFim;

    @Schema(description = "ID do tenant")
    private String tenantId;

    // Getters e Setters
    public Optional<String> getNome() { return nome; }
    public void setNome(String nome) { this.nome = Optional.ofNullable(nome); }

    public Optional<Boolean> getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = Optional.ofNullable(ativo); }

    public Optional<LocalDateTime> getDataCriacaoInicio() { return dataCriacaoInicio; }
    public void setDataCriacaoInicio(LocalDateTime dataCriacaoInicio) {
        this.dataCriacaoInicio = Optional.ofNullable(dataCriacaoInicio);
    }

    public Optional<LocalDateTime> getDataCriacaoFim() { return dataCriacaoFim; }
    public void setDataCriacaoFim(LocalDateTime dataCriacaoFim) {
        this.dataCriacaoFim = Optional.ofNullable(dataCriacaoFim);
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
```

---

## Port de Entrada (Use Case)

```java
package br.com.empresa.projeto.core.application.port.in;

import br.com.empresa.projeto.core.application.dto.CreateExampleDTO;
import br.com.empresa.projeto.core.application.dto.ExampleDTO;
import br.com.empresa.projeto.core.application.dto.UpdateExampleDTO;

public interface CreateExampleUseCase {
    ExampleDTO create(CreateExampleDTO dto);
}

public interface UpdateExampleUseCase {
    ExampleDTO update(String id, UpdateExampleDTO dto);
}

public interface DeleteExampleUseCase {
    void delete(String id);
}

public interface FindExampleUseCase {
    ExampleDTO findById(String id);
    PageResponseDTO<ExampleDTO> findAll(int page, int size);
}
```

---

## Port de Saída para Serviço Externo

```java
package br.com.empresa.projeto.core.application.port.out;

import br.com.empresa.projeto.core.domain.entity.Example;

public interface ExampleNotificationPort {

    void notifyCreated(Example example);

    void notifyUpdated(Example example);

    void notifyDeleted(String exampleId);
}
```

---

## Port com Retorna Paginado

```java
package br.com.empresa.projeto.core.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import br.com.empresa.projeto.core.domain.entity.Example;

public interface ExamplePersistencePort {

    Page<Example> findAll(Pageable pageable);

    Page<Example> findByAtivoTrue(Pageable pageable);

    Page<Example> findByTenantId(String tenantId, Pageable pageable);
}
```

---

## Port com Especificação

```java
package br.com.empresa.projeto.core.application.port.out;

import br.com.empresa.projeto.core.domain.entity.Example;
import java.util.List;
import java.util.Optional;

public interface ExamplePersistencePort {

    // CRUD básico
    Example save(Example example);
    Optional<Example> findById(String id);
    List<Example> findAll();
    void deleteById(String id);
    boolean existsById(String id);

    // Queries específicas do domínio
    Optional<Example> findByCodigo(String codigo, String tenantId);
    List<Example> findByCategoria(String categoria, String tenantId);
    List<Example> findAtivosByTenantId(String tenantId);

    // Queries com filtros
    List<Example> findByNomeContainingAndAtivo(String nome, Boolean ativo, String tenantId);
}
```

---

## Relação: Port → Adapter → Repository

```
┌─────────────────┐
│   Service       │
│  (Use Case)     │
└────────┬────────┘
         | implementa
         ▼
┌─────────────────┐      ┌──────────────────┐
│   Port IN       │      │  Port OUT        │
│ (UseCase)       │      │ (Persistence)    │
└─────────────────┘      └────────┬─────────┘
                                   | implementa
                                   ▼
                         ┌──────────────────┐
                         │   Adapter        │
                         │ (QueryDSL)       │
                         └────────┬─────────┘
                                  | usa
                                  ▼
                         ┌──────────────────┐
                         │  Repository      │
                         │  (JPA)           │
                         └──────────────────┘
```

---

## Boas Práticas

| Prática | Descrição |
|---------|-----------|
| **Interfaces puras** | Ports são apenas interfaces |
| **Nome descritivo** | `XxxPersistencePort`, `CreateXxxUseCase` |
| **Parâmetros de domínio** | Receber Domain Objects, não Entities |
| **Retorno de domínio** | Retornar Domain Objects, não Entities |
| **Ports IN nos Controllers** | Controllers injetam Use Cases |
| **Ports OUT nos Adapters** | Adapters implementam Persistence Ports |

---

**IMPORTANTE**: Ports definem O QUE fazer, não COMO fazer. A implementação fica no Adapter.
