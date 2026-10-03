package br.com.archbase.boilerplate.rest.infrastructure.filter;

import br.com.archbase.boilerplate.rest.infrastructure.error.ApiErrorWriter;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TenantAccessValidationFilterTest {

    public record ComTenant(String tenantId) {
        public String getTenantId() {
            return tenantId;
        }
    }

    private final TenantAccessValidationFilter filter = new TenantAccessValidationFilter(
            new ApiErrorWriter(new ObjectMapper().registerModule(new JavaTimeModule())));
    private final FilterChain chain = mock(FilterChain.class);
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/v1/produtos");
        response = new MockHttpServletResponse();
        ArchbaseTenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        ArchbaseTenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void autenticarCom(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "x", List.of()));
    }

    @Test
    void liberaQuandoTenantDoUsuarioEIgualAoSolicitado() throws Exception {
        autenticarCom(new ComTenant("tenant-a"));
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void negaQuandoTenantDoUsuarioDifere() throws Exception {
        autenticarCom(new ComTenant("tenant-b"));
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("\"status\":\"FORBIDDEN\"", "\"path\":\"/api/v1/produtos\"",
                        "Acesso negado ao tenant solicitado");
    }

    @Test
    void negaQuandoUsuarioNaoTemTenant() throws Exception {
        autenticarCom(new ComTenant(" "));
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void negaQuandoPrincipalNaoExpoeTenant() throws Exception {
        autenticarCom("usuario-sem-getTenantId");
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void naoValidaCaminhoPublico() throws Exception {
        request = new MockHttpServletRequest("GET", "/actuator/health");
        autenticarCom("qualquer");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
