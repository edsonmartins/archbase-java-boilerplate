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

    /**
     * O parâmetro {@code WebClient.Builder} foi removido.
     *
     * <p>Ele era exigido por injeção e <b>nunca usado</b> — o corpo chama
     * {@code WebClient.builder()} direto. Como esse bean só existe com a
     * autoconfiguração do WebFlux, que um projeto servlet não traz, a aplicação
     * inteira não subia:
     *
     * <pre>NoSuchBeanDefinitionException: No qualifying bean of type '...WebClient$Builder'</pre>
     *
     * <p>Um parâmetro não usado derrubando a aplicação é o pior tipo de
     * acoplamento: nem o compilador nem a leitura do corpo denunciam.
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }
}
