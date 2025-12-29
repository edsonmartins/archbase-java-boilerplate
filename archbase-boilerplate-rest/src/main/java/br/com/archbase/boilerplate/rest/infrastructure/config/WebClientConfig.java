package br.com.archbase.boilerplate.rest.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuração do WebClient para chamadas HTTP reativas.
 *
 * <p>WebClient é o cliente HTTP recomendado para aplicações reativas,
 * substituindo o RestTemplate em cenários não-bloqueantes.</p>
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return WebClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }
}
