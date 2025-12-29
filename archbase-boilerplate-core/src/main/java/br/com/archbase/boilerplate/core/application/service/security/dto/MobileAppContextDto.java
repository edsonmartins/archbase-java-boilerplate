package br.com.archbase.boilerplate.core.application.service.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO com dados específicos para contexto de autenticação MOBILE_APP.
 * <p>
 * Utilizado para enriquecer a resposta de autenticação com informações
 * relevantes para usuários do aplicativo móvel (motoristas, operadores, etc).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileAppContextDto {

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
     * Número de telefone (para contatos via WhatsApp/SMS).
     */
    private String phoneNumber;

    /**
     * ID do tenant (contexto multi-tenancy).
     */
    private String tenantId;
}
