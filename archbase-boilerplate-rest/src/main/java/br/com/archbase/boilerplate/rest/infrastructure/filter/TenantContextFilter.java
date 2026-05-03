package br.com.archbase.boilerplate.rest.infrastructure.filter;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro para extrair o tenant ID do header X-TENANT-ID e configurar o contexto multi-tenant.
 *
 * <p>Este filtro é executado antes dos filtros de segurança para garantir que o
 * tenant esteja configurado para todas as requisições.</p>
 */
@Component
@Order(1)
@Slf4j
public class TenantContextFilter implements Filter {

    private static final String TENANT_HEADER = "X-TENANT-ID";
    private static final String TENANT_HEADER_ALT = "X-Tenant-Id";

    @Value("${archbase.app.tenant.default.id:}")
    private String defaultTenantId;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            String tenantId = extractTenantId(httpRequest);

            if (tenantId != null && !tenantId.isBlank()) {
                ArchbaseTenantContext.setTenantId(tenantId);
                log.debug("Tenant context configurado: {}", tenantId);
            } else if (defaultTenantId != null && !defaultTenantId.isBlank()) {
                ArchbaseTenantContext.setTenantId(defaultTenantId);
                log.debug("Usando tenant padrão: {}", defaultTenantId);
            }

            chain.doFilter(request, response);
        } finally {
            ArchbaseTenantContext.clear();
        }
    }

    private String extractTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = request.getHeader(TENANT_HEADER_ALT);
        }
        return tenantId;
    }
}
