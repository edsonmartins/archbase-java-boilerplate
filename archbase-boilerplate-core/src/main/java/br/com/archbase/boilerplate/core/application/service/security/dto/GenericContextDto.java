package br.com.archbase.boilerplate.core.application.service.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico para contexto de autenticação não reconhecido.
 * <p>
 * Utilizado como fallback quando o contexto de autenticação não é
 * especificamente suportado, garantindo que uma resposta válida seja retornada.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericContextDto {

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
     * Tipo de contexto recebido na requisição.
     */
    private String contextType;

    /**
     * Email do usuário.
     */
    private String email;

    /**
     * ID do tenant (contexto multi-tenancy).
     */
    private String tenantId;
}
