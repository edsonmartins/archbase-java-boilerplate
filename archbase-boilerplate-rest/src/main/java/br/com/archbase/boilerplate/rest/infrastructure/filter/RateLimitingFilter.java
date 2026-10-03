package br.com.archbase.boilerplate.rest.infrastructure.filter;

import br.com.archbase.boilerplate.rest.infrastructure.config.RateLimitingConfig;
import br.com.archbase.boilerplate.rest.infrastructure.error.ApiErrorWriter;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Aplica os limites de {@link RateLimitingConfig}: webhooks e requisições anônimas por IP,
 * requisições autenticadas por usuário. Excedido o limite, responde 429 com {@code Retry-After}.
 *
 * <p>Roda depois da cadeia do Spring Security (para enxergar o usuário autenticado) e usa só
 * {@code getRemoteAddr()}: confiar em X-Forwarded-For sem um proxy que o sobrescreva deixaria o
 * cliente escolher a própria identidade e esgotar o bucket de terceiros. Atrás de proxy, configure
 * o {@code server.forward-headers-strategy} para que o endereço correto chegue aqui.
 */
@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String WEBHOOK_PREFIX = "/api/v1/webhooks";

    private final RateLimitingConfig config;
    private final ApiErrorWriter apiErrorWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!config.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Bucket bucket = resolveBucket(request);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
        log.warn("Rate limit excedido: {} {} de {}", request.getMethod(), request.getRequestURI(),
                request.getRemoteAddr());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        apiErrorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                "Limite de requisições excedido. Tente novamente em " + retryAfterSeconds + "s");
    }

    private Bucket resolveBucket(HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        if (request.getRequestURI().startsWith(WEBHOOK_PREFIX)) {
            return config.webhookBucket(ip);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return config.authenticatedBucket(authentication.getName());
        }
        return config.ipBucket(ip);
    }
}
