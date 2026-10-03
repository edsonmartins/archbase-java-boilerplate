package br.com.archbase.boilerplate.rest.infrastructure.filter;

import br.com.archbase.boilerplate.rest.infrastructure.config.RateLimitingConfig;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitingFilterTest {

    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // webhook 2/min, autenticado 3/min, ip 2/min
    private RateLimitingFilter filter(boolean enabled) {
        return new RateLimitingFilter(new RateLimitingConfig(enabled, 2, 60, 3, 60, 2, 60));
    }

    private MockHttpServletResponse chamar(RateLimitingFilter f, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        f.doFilter(request, response, chain);
        return response;
    }

    @Test
    void anonimoExcedeLimitePorIpERecebe429ComRetryAfter() throws Exception {
        RateLimitingFilter f = filter(true);
        chamar(f, "/api/v1/produtos", "10.0.0.1");
        chamar(f, "/api/v1/produtos", "10.0.0.1");
        MockHttpServletResponse terceira = chamar(f, "/api/v1/produtos", "10.0.0.1");

        assertThat(terceira.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(terceira.getHeader("Retry-After"))).isPositive();
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ipsDiferentesTemBucketsIndependentes() throws Exception {
        RateLimitingFilter f = filter(true);
        chamar(f, "/api/v1/produtos", "10.0.0.1");
        chamar(f, "/api/v1/produtos", "10.0.0.1");
        assertThat(chamar(f, "/api/v1/produtos", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void autenticadoUsaLimitePorUsuarioENaoPorIp() throws Exception {
        RateLimitingFilter f = filter(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana", "x", List.of()));
        for (int i = 0; i < 3; i++) {
            assertThat(chamar(f, "/api/v1/produtos", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        assertThat(chamar(f, "/api/v1/produtos", "10.0.0.1").getStatus()).isEqualTo(429);
    }

    @Test
    void webhookTemBucketProprio() throws Exception {
        RateLimitingFilter f = filter(true);
        chamar(f, "/api/v1/webhooks/x", "10.0.0.1");
        chamar(f, "/api/v1/webhooks/x", "10.0.0.1");
        assertThat(chamar(f, "/api/v1/webhooks/x", "10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(chamar(f, "/api/v1/produtos", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void actuatorEDesligadoNaoSaoLimitados() throws Exception {
        RateLimitingFilter f = filter(true);
        for (int i = 0; i < 5; i++) {
            assertThat(chamar(f, "/actuator/health", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        RateLimitingFilter desligado = filter(false);
        for (int i = 0; i < 5; i++) {
            assertThat(chamar(desligado, "/api/v1/produtos", "10.0.0.9").getStatus()).isEqualTo(200);
        }
    }
}
