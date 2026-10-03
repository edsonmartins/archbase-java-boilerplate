package br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity;

import br.com.archbase.boilerplate.core.application.dto.ProdutoDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProdutoEntityTest {

    @Test
    void conversaoDeIdaEVoltaPreservaMarcaUrlImagemEDestaque() {
        ProdutoDTO dto = ProdutoDTO.builder()
                .nome("Notebook")
                .marca("Acme")
                .urlImagem("https://exemplo.com/n.png")
                .destaque(true)
                .build();

        ProdutoEntity entity = ProdutoEntity.fromDTO(dto);

        assertThat(entity.getMarca()).isEqualTo("Acme");
        assertThat(entity.getUrlImagem()).isEqualTo("https://exemplo.com/n.png");
        assertThat(entity.getDestaque()).isTrue();

        ProdutoDTO volta = entity.toDTO();

        assertThat(volta.getMarca()).isEqualTo("Acme");
        assertThat(volta.getUrlImagem()).isEqualTo("https://exemplo.com/n.png");
        assertThat(volta.getDestaque()).isTrue();
    }
}
