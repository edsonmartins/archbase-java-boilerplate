package br.com.archbase.boilerplate.core.application.service.security;

import br.com.archbase.security.auth.AuthenticationBusinessDelegate;
import br.com.archbase.security.auth.AuthenticationResponse;
import br.com.archbase.security.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Implementação do AuthenticationBusinessDelegate para o Archbase Boilerplate.
 * <p>
 * Este componente conecta a infraestrutura de autenticação do Archbase
 * com a lógica de negócio do boilerplate.
 * </p>
 */
@Component
@Slf4j
public class BoilerplateAuthenticationBusinessDelegate implements AuthenticationBusinessDelegate {

    private static final List<String> SUPPORTED_CONTEXTS = Arrays.asList(
            "WEB_ADMIN", "MOBILE_APP", "API"
    );

    @Override
    public String onUserRegistered(User user, Map<String, Object> registrationData) {
        try {
            log.debug("Usuário registrado no boilerplate: {}", user.getEmail());
            // Implementar lógica de criação de dados de negócio do usuário se necessário
            return user.getId().toString();
        } catch (Exception e) {
            log.error("Erro ao processar registro de usuário: {}", user.getEmail(), e);
            throw new RuntimeException("Falha ao criar dados de negócio do usuário", e);
        }
    }

    @Override
    public AuthenticationResponse enrichAuthenticationResponse(
            AuthenticationResponse baseResponse,
            String context,
            HttpServletRequest request) {
        try {
            log.debug("Enriquecendo resposta de autenticação para contexto: {}", context);
            // Retornar a resposta base sem modificações
            return baseResponse;
        } catch (Exception e) {
            log.error("Erro ao enriquecer resposta de autenticação para contexto: {}", context, e);
            return baseResponse;
        }
    }

    @Override
    public boolean supportsContext(String context) {
        return SUPPORTED_CONTEXTS.contains(context);
    }

    @Override
    public List<String> getSupportedContexts() {
        return SUPPORTED_CONTEXTS;
    }

    @Override
    public void preAuthenticate(String email, String context) {
        try {
            log.debug("Validação pré-autenticação para email: {} e contexto: {}", email, context);
            // Implementar validações pré-autenticação se necessário
        } catch (Exception e) {
            log.error("Erro na validação pré-autenticação", e);
            throw e;
        }
    }

    @Override
    public void postAuthenticate(User user, String context) {
        try {
            log.debug("Ações pós-autenticação para usuário: {} e contexto: {}", user.getEmail(), context);
            // Implementar ações pós-autenticação se necessário
        } catch (Exception e) {
            log.error("Erro em ações pós-autenticação", e);
            // Não falhar para não impactar o login
        }
    }

    @Override
    public String onSocialLogin(String provider, Map<String, Object> providerData) {
        try {
            log.debug("Processando login social via provider: {}", provider);
            String email = (String) providerData.get("email");
            String name = (String) providerData.get("name");

            if (email == null || name == null) {
                throw new RuntimeException("Dados obrigatórios do provedor social não fornecidos");
            }
            // Retornar null indica que o usuário ainda não existe
            return null;
        } catch (Exception e) {
            log.error("Erro no login social via provider: {}", provider, e);
            throw new RuntimeException("Falha no login social", e);
        }
    }
}
