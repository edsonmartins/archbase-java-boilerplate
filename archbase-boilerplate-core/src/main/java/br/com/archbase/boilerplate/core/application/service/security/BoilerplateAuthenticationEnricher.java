package br.com.archbase.boilerplate.core.application.service.security;

import br.com.archbase.boilerplate.core.application.service.security.dto.GenericContextDto;
import br.com.archbase.boilerplate.core.application.service.security.dto.MobileAppContextDto;
import br.com.archbase.boilerplate.core.application.service.security.dto.WebAdminContextDto;
import br.com.archbase.security.auth.AuthenticationResponse;
import br.com.archbase.security.domain.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Enricher responsável por enriquecer a resposta de autenticação com dados
 * específicos de cada contexto do Archbase Boilerplate.
 * <p>
 * Suporta os contextos:
 * - WEB_ADMIN: Interface administrativa web
 * - MOBILE_APP: Aplicativo móvel
 * - API: Integrações via API
 * - Fallback para contextos não reconhecidos
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BoilerplateAuthenticationEnricher {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{8,}$");

    /**
     * Enriquece a resposta de autenticação baseado no contexto específico.
     *
     * @param baseResponse Resposta base de autenticação do Archbase
     * @param context      Contexto da autenticação (WEB_ADMIN, MOBILE_APP, etc)
     * @param request      Requisição HTTP para obter headers adicionais
     * @return Resposta enriquecida com dados do contexto
     */
    public AuthenticationResponse enrich(AuthenticationResponse baseResponse,
                                        String context,
                                        HttpServletRequest request) {

        if (!supports(context)) {
            log.debug("Contexto {} não suportado, retornando resposta básica", context);
            return baseResponse;
        }

        try {
            User user = baseResponse.getUser();
            Object contextData = null;

            switch (context) {
                case "WEB_ADMIN":
                    contextData = enrichWebAdminContext(user, request);
                    break;
                case "MOBILE_APP":
                    contextData = enrichMobileAppContext(user, request);
                    break;
                default:
                    log.info("Aplicando fallback para contexto não implementado: {}", context);
                    contextData = enrichGenericContext(user, context, request);
                    break;
            }

            if (contextData != null) {
                log.info("Contexto {} enriquecido com sucesso para usuário: {}", context, user.getEmail());
                baseResponse.setContext(contextData);
                return baseResponse;
            }
            return baseResponse;

        } catch (Exception e) {
            log.error("Erro ao enriquecer contexto {}: {}", context, e.getMessage(), e);
            return baseResponse;
        }
    }

    /**
     * Verifica se o contexto é suportado por este enricher.
     *
     * @param context Contexto a verificar
     * @return true se suportado, false caso contrário
     */
    public boolean supports(String context) {
        if (Arrays.asList("WEB_ADMIN", "MOBILE_APP", "API").contains(context)) {
            return true;
        }
        return context != null && !context.trim().isEmpty();
    }

    /**
     * Enriquece contexto para interface web administrativa.
     */
    private WebAdminContextDto enrichWebAdminContext(User user, HttpServletRequest request) {
        try {
            log.debug("Enriquecendo contexto WEB_ADMIN para: {}", user.getEmail());

            return WebAdminContextDto.builder()
                    .userId(user.getId().toString())
                    .fullName(user.getName())
                    .role(resolveRole(user))
                    .email(user.getEmail())
                    .profileCode(resolveProfileCode(user))
                    .tenantId(resolveTenant(request))
                    .build();

        } catch (Exception e) {
            log.error("Erro ao enriquecer contexto WEB_ADMIN: {}", e.getMessage(), e);
            return WebAdminContextDto.builder()
                    .userId(user.getId().toString())
                    .fullName(user.getName())
                    .email(user.getEmail())
                    .build();
        }
    }

    /**
     * Enriquece contexto para aplicativo mobile.
     */
    private MobileAppContextDto enrichMobileAppContext(User user, HttpServletRequest request) {
        try {
            log.debug("Enriquecendo contexto MOBILE_APP para: {}", user.getEmail());

            return MobileAppContextDto.builder()
                    .userId(user.getId().toString())
                    .fullName(user.getName())
                    .role(resolveRole(user))
                    .email(user.getEmail())
                    .profileCode(resolveProfileCode(user))
                    .phoneNumber(resolvePhone(user))
                    .tenantId(resolveTenant(request))
                    .build();

        } catch (Exception e) {
            log.error("Erro ao enriquecer contexto MOBILE_APP: {}", e.getMessage(), e);
            return MobileAppContextDto.builder()
                    .userId(user.getId().toString())
                    .fullName(user.getName())
                    .email(user.getEmail())
                    .build();
        }
    }

    /**
     * Sistema de fallback para contextos não implementados.
     */
    private GenericContextDto enrichGenericContext(User user, String context, HttpServletRequest request) {
        try {
            log.info("Enriquecendo contexto genérico: {} para usuário: {}", context, user.getEmail());

            return GenericContextDto.builder()
                    .userId(user.getId().toString())
                    .fullName(user.getName())
                    .role(resolveRole(user))
                    .contextType(context)
                    .email(user.getEmail())
                    .tenantId(resolveTenant(request))
                    .build();

        } catch (Exception e) {
            log.error("Erro ao enriquecer contexto genérico {}: {}", context, e.getMessage(), e);
            return GenericContextDto.builder()
                    .userId(user.getId().toString())
                    .fullName(user.getName())
                    .role(resolveRole(user))
                    .contextType(context)
                    .email(user.getEmail())
                    .build();
        }
    }

    /**
     * Resolve a role do usuário baseado nas informações do Archbase.
     */
    private String resolveRole(User user) {
        if (Boolean.TRUE.equals(user.getIsAdministrator())) {
            return "ADMIN";
        }
        return user.getGroups() != null && !user.getGroups().isEmpty()
                ? Optional.ofNullable(user.getGroups().get(0).getGroup())
                .map(g -> g.getName() != null ? g.getName().toUpperCase() : "USER")
                .orElse("USER")
                : "USER";
    }

    /**
     * Resolve o código de perfil do usuário.
     */
    private String resolveProfileCode(User user) {
        if (user.getProfile() != null && user.getProfile().getName() != null) {
            return user.getProfile().getName().toUpperCase();
        }
        if (user.getGroups() != null && !user.getGroups().isEmpty()) {
            return Optional.ofNullable(user.getGroups().get(0).getGroup())
                    .map(g -> g.getName() != null ? g.getName().toUpperCase() : null)
                    .orElse("DEFAULT");
        }
        return "DEFAULT";
    }

    /**
     * Resolve o número de telefone do usuário.
     */
    private String resolvePhone(User user) {
        if (user.getUserName() != null && PHONE_PATTERN.matcher(user.getUserName()).matches()) {
            return user.getUserName();
        }
        if (user.getNickname() != null && PHONE_PATTERN.matcher(user.getNickname()).matches()) {
            return user.getNickname();
        }
        return null;
    }

    /**
     * Resolve o tenant ID do header X-Tenant-Id.
     */
    private String resolveTenant(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String header = request.getHeader("X-Tenant-Id");
        if (header != null && !header.isBlank()) {
            return header;
        }

        // Tenta variação com letra minúscula
        header = request.getHeader("X-tenant-id");
        if (header != null && !header.isBlank()) {
            return header;
        }

        return null;
    }
}
