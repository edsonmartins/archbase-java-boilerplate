package br.com.archbase.boilerplate.rest.infrastructure.filter;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.boilerplate.rest.infrastructure.error.ApiErrorWriter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
@RequiredArgsConstructor
public class TenantAccessValidationFilter implements Filter {

    private final ApiErrorWriter apiErrorWriter;

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
                apiErrorWriter.write(httpRequest, httpResponse, HttpStatus.FORBIDDEN,
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

        // Anônimo não tem tenant para comparar (o principal é a String "anonymousUser"): quem decide
        // se a rota exige login é a cadeia do Spring Security. Validar aqui negaria o próprio login.
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
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

    /**
     * Falha fechada: só libera quando o tenant do usuário pôde ser lido e é igual ao solicitado.
     * Principal ausente, sem {@code getTenantId}, com tenant em branco ou erro na leitura negam o
     * acesso — liberar nesses casos deixaria qualquer usuário autenticado ler outro tenant bastando
     * trocar o header X-TENANT-ID.
     */
    private boolean hasAccessToTenant(Authentication authentication, String requestedTenantId) {
        Object principal = authentication.getPrincipal();

        if (principal == null) {
            log.warn("Usuário {} sem principal; negando acesso ao tenant {}",
                    authentication.getName(), requestedTenantId);
            return false;
        }

        // Reflection para ser compatível com diferentes implementações de UserDetails
        // (o UserEntity do archbase-security expõe getTenantId).
        String userTenantId;
        try {
            userTenantId = (String) principal.getClass().getMethod("getTenantId").invoke(principal);
        } catch (NoSuchMethodException e) {
            log.warn("Principal {} não possui getTenantId; negando acesso ao tenant {}",
                    principal.getClass().getName(), requestedTenantId);
            return false;
        } catch (Exception e) {
            log.warn("Erro ao verificar tenant do usuário {}; negando acesso: {}",
                    authentication.getName(), e.getMessage());
            return false;
        }

        if (userTenantId == null || userTenantId.isBlank()) {
            log.warn("Usuário {} não possui tenant associado; negando acesso ao tenant {}",
                    authentication.getName(), requestedTenantId);
            return false;
        }

        boolean hasAccess = userTenantId.equals(requestedTenantId);
        if (!hasAccess) {
            log.debug("Usuário {} pertence ao tenant {}, mas solicitou acesso ao tenant {}",
                    authentication.getName(), userTenantId, requestedTenantId);
        }
        return hasAccess;
    }
}
