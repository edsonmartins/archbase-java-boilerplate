package br.com.archbase.boilerplate.core.application.service.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO com dados específicos para contexto de autenticação WEB_ADMIN.
 * <p>
 * Utilizado para enriquecer a resposta de autenticação com informações
 * relevantes para usuários da interface administrativa web.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAdminContextDto {

    /**
     * ID do usuário autenticado.
     */
    private String userId;

    /**
     * Nome completo do usuário.
     */
    private String fullName;

    /**
     * Role do usuário no sistema.
     */
    private String role;

    /**
     * Email do usuário.
     */
    private String email;

    /**
     * Código do perfil de acesso.
     */
    private String profileCode;

    /**
     * ID do tenant (contexto multi-tenancy).
     */
    private String tenantId;
}
