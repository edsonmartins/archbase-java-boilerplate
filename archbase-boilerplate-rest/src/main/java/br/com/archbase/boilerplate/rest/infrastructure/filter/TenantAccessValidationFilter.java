package br.com.archbase.boilerplate.rest.infrastructure.filter;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro para validar se o usuário autenticado tem acesso ao tenant solicitado.
 *
 * <p>Este filtro é executado após a autenticação para garantir que o usuário
 * só possa acessar dados do tenant ao qual pertence.</p>
 */
@Component
@Order(2)
@Slf4j
public class TenantAccessValidationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestedTenantId = ArchbaseTenantContext.getTenantId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (shouldValidateTenant(httpRequest, authentication, requestedTenantId)) {
            if (!hasAccessToTenant(authentication, requestedTenantId)) {
                log.warn("Acesso negado: usuário {} tentou acessar tenant {}",
                        authentication.getName(), requestedTenantId);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Acesso negado ao tenant solicitado");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean shouldValidateTenant(HttpServletRequest request,
                                         Authentication authentication,
                                         String requestedTenantId) {
        if (requestedTenantId == null || requestedTenantId.isBlank()) {
            return false;
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            return false;
        }

        return true;
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api/v1/public");
    }

    private boolean hasAccessToTenant(Authentication authentication, String requestedTenantId) {
        Object principal = authentication.getPrincipal();

        if (principal == null) {
            return true;
        }

        // Tenta obter o tenant do principal usando reflection para ser compatível
        // com diferentes implementações de UserDetails
        try {
            var method = principal.getClass().getMethod("getTenantId");
            String userTenantId = (String) method.invoke(principal);

            if (userTenantId == null || userTenantId.isBlank()) {
                log.debug("Usuário {} não possui tenant associado, permitindo acesso",
                        authentication.getName());
                return true;
            }

            boolean hasAccess = userTenantId.equals(requestedTenantId);
            if (!hasAccess) {
                log.debug("Usuário {} pertence ao tenant {}, mas solicitou acesso ao tenant {}",
                        authentication.getName(), userTenantId, requestedTenantId);
            }
            return hasAccess;
        } catch (NoSuchMethodException e) {
            // Se não tem método getTenantId, permite acesso
            log.debug("Principal não possui método getTenantId, permitindo acesso");
            return true;
        } catch (Exception e) {
            log.warn("Erro ao verificar tenant do usuário: {}", e.getMessage());
            return true;
        }
    }
}
