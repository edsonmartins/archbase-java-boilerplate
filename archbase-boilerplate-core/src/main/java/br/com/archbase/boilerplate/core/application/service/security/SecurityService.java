package br.com.archbase.boilerplate.core.application.service.security;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.adapter.SecurityAdapter;
import br.com.archbase.security.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço de validação de segurança e propriedade de recursos.
 * <p>
 * Este serviço fornece métodos para validações de autorização que podem ser
 * usados programaticamente ou em conjunto com as anotações do Archbase.
 * </p>
 * <p>
 * NOTA: Para autorização básica por roles, prefira usar as anotações do Archbase:
 * {@link br.com.archbase.security.annotations.RequireRole}
 * {@link br.com.archbase.security.annotations.RequireProfile}
 * {@link br.com.archbase.security.annotations.RequirePersona}
 * </p>
 * <p>
 * Exemplos de uso programático:
 * <pre>
 * {@code
 * @Service
 * @RequiredArgsConstructor
 * public class ExampleService {
 *     private final SecurityService securityService;
 *
 *     public void performAction() {
 *         if (securityService.isAdmin()) {
 *             // Lógica específica para ADMIN
 *         }
 *         String userId = securityService.getCurrentUserId();
 *         String tenantId = securityService.getCurrentTenantId();
 *     }
 * }
 * }
 * </pre>
 * </p>
 */
@Service("securityService")
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

    private final SecurityAdapter securityAdapter;

    /**
     * Verifica se o usuário atual tem a role especificada.
     *
     * @param role Role a verificar
     * @return true se usuário tem a role
     */
    public boolean hasRole(String role) {
        try {
            User user = securityAdapter.getLoggedUser();
            if (user == null) {
                return false;
            }
            if (Boolean.TRUE.equals(user.getIsAdministrator()) && "ADMIN".equals(role)) {
                return true;
            }
            return user.getGroups() != null && !user.getGroups().isEmpty()
                    && user.getGroups().stream()
                    .anyMatch(g -> g.getGroup() != null
                            && role.equals(g.getGroup().getName()));
        } catch (Exception e) {
            log.error("Erro ao verificar role {}: {}", role, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifica se o usuário atual tem o perfil especificado.
     *
     * @param profile Perfil a verificar
     * @return true se usuário tem o perfil
     */
    public boolean hasProfile(String profile) {
        try {
            User user = securityAdapter.getLoggedUser();
            if (user == null || user.getProfile() == null) {
                return false;
            }
            return profile.equals(user.getProfile().getName());
        } catch (Exception e) {
            log.error("Erro ao verificar profile {}: {}", profile, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifica se recurso pertence ao usuário (genérico).
     * <p>
     * Útil para validações simples onde o recurso tem um campo "criadoPor" ou similar.
     * </p>
     *
     * @param resourceOwner Dono do recurso (username ou userId)
     * @param currentUser    Username ou userId atual
     * @return true se recurso pertence ao usuário
     */
    public boolean isOwner(String resourceOwner, String currentUser) {
        boolean isOwner = currentUser != null && currentUser.equals(resourceOwner);
        log.debug("Verificando ownership: resource={}, current={}, result={}",
                resourceOwner, currentUser, isOwner);
        return isOwner;
    }

    /**
     * Verifica se o usuário atual é ADMIN.
     *
     * @return true se usuário é ADMIN
     */
    public boolean isAdmin() {
        try {
            User user = securityAdapter.getLoggedUser();
            return user != null && Boolean.TRUE.equals(user.getIsAdministrator());
        } catch (Exception e) {
            log.error("Erro ao verificar se usuário é ADMIN: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifica se usuário tem permissão OR (qualquer uma das condições).
     * <p>
     * Útil para validações complexas onde múltiplas condições são aceitas.
     * </p>
     *
     * @param conditions Array de condições booleanas
     * @return true se pelo menos uma condição é verdadeira
     */
    public boolean anyOf(boolean... conditions) {
        for (boolean condition : conditions) {
            if (condition) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se usuário tem permissão AND (todas as condições).
     *
     * @param conditions Array de condições booleanas
     * @return true se todas as condições são verdadeiras
     */
    public boolean allOf(boolean... conditions) {
        for (boolean condition : conditions) {
            if (!condition) {
                return false;
            }
        }
        return true;
    }

    /**
     * Obtém o usuário autenticado.
     *
     * @return User ou null se não autenticado
     */
    public User getCurrentUser() {
        try {
            return securityAdapter.getLoggedUser();
        } catch (Exception e) {
            log.error("Erro ao obter usuário: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtém o ID do usuário autenticado.
     *
     * @return ID do usuário ou null se não autenticado
     */
    public String getCurrentUserId() {
        try {
            User user = securityAdapter.getLoggedUser();
            return user != null ? user.getId().toString() : null;
        } catch (Exception e) {
            log.error("Erro ao obter ID do usuário: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtém o username do usuário autenticado.
     *
     * @return Username ou null se não autenticado
     */
    public String getCurrentUsername() {
        try {
            User user = securityAdapter.getLoggedUser();
            return user != null ? user.getEmail() : null;
        } catch (Exception e) {
            log.error("Erro ao obter username: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtém o nome do usuário autenticado.
     *
     * @return Nome ou null se não autenticado
     */
    public String getCurrentUserName() {
        try {
            User user = securityAdapter.getLoggedUser();
            return user != null ? user.getName() : null;
        } catch (Exception e) {
            log.error("Erro ao obter nome do usuário: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtém o ID do tenant atual.
     *
     * @return ID do tenant ou null se não disponível
     */
    public String getCurrentTenantId() {
        try {
            return ArchbaseTenantContext.getTenantId();
        } catch (Exception e) {
            log.error("Erro ao obter ID do tenant: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtém a role atual do usuário.
     *
     * @return Role ou null se não disponível
     */
    public String getCurrentRole() {
        try {
            User user = securityAdapter.getLoggedUser();
            if (user == null) {
                return null;
            }
            if (Boolean.TRUE.equals(user.getIsAdministrator())) {
                return "ADMIN";
            }
            return user.getGroups() != null && !user.getGroups().isEmpty()
                    && user.getGroups().get(0).getGroup() != null
                    ? user.getGroups().get(0).getGroup().getName()
                    : null;
        } catch (Exception e) {
            log.error("Erro ao obter role atual: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtém o perfil atual do usuário.
     *
     * @return Perfil ou null se não disponível
     */
    public String getCurrentProfile() {
        try {
            User user = securityAdapter.getLoggedUser();
            return user != null && user.getProfile() != null
                    ? user.getProfile().getName()
                    : null;
        } catch (Exception e) {
            log.error("Erro ao obter perfil atual: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Verifica se o usuário pode acessar o recurso (baseado em tenant).
     * <p>
     * Para recursos multi-tenant, verifica se o resourceTenantId corresponde
     * ao tenant do usuário autenticado.
     * </p>
     *
     * @param resourceTenantId Tenant do recurso
     * @return true se usuário pode acessar o recurso
     */
    public boolean canAccessTenant(String resourceTenantId) {
        String currentUserTenant = getCurrentTenantId();
        boolean canAccess = currentUserTenant != null && currentUserTenant.equals(resourceTenantId);
        log.debug("Verificando acesso tenant: resource={}, current={}, result={}",
                resourceTenantId, currentUserTenant, canAccess);
        return canAccess;
    }
}
