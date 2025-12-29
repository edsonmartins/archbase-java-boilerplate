package br.com.archbase.boilerplate.rest.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração global de CORS para a API.
 */
@Configuration
public class GlobalCorsConfig {

    @Value("${application.security.cors.allowed-origins:http://localhost:3000,http://localhost:4200}")
    private String[] allowedOrigins;

    @Value("${application.security.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String[] allowedMethods;

    @Value("${application.security.cors.allowed-headers:Authorization,Content-Type,Accept,X-Requested-With,X-Device-ID,X-App-Version,X-TENANT-ID,X-Tenant-Id}")
    private String[] allowedHeaders;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(Arrays.asList(allowedMethods));
        config.setAllowedHeaders(Arrays.asList(allowedHeaders));
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
