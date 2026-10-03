package br.com.archbase.boilerplate.rest.infrastructure.error;

import br.com.archbase.boilerplate.core.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void preencheOPathSemQueryString() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/produtos/42");
        request.setQueryString("token=segredo");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ResponseEntity<Object> resposta = handler.handleEntityNotFound(new EntityNotFoundException("Produto", "42"));

        ApiError erro = (ApiError) resposta.getBody();
        assertThat(erro.getPath()).isEqualTo("/api/v1/produtos/42");
    }

    @Test
    void semRequisicaoEmCursoMantemPathNulo() {
        ResponseEntity<Object> resposta = handler.handleEntityNotFound(new EntityNotFoundException("Produto", "42"));

        assertThat(((ApiError) resposta.getBody()).getPath()).isNull();
    }
}
