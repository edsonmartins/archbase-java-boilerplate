package br.com.archbase.boilerplate.rest.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuração do RestTemplate para chamadas HTTP síncronas.
 *
 * <p>RestTemplate é o cliente HTTP tradicional do Spring.
 * Para novos projetos, considere usar WebClient.</p>
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
