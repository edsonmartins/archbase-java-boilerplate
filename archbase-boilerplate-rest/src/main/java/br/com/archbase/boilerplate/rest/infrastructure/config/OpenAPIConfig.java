package br.com.archbase.boilerplate.rest.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do OpenAPI/Swagger para documentação da API.
 */
@Configuration
public class OpenAPIConfig {

    @Value("${openapi.server.dev.url:http://localhost:8080}")
    private String devUrl;

    @Value("${openapi.server.dev.description:Development server}")
    private String devDescription;

    @Bean
    public OpenAPI archbaseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Archbase Boilerplate API")
                        .description("API REST para gestão de produtos com Archbase Framework")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Archbase Team")
                                .email("contact@archbase.com.br")
                                .url("https://archbase.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url(devUrl)
                                .description(devDescription)
                ));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/admin/**")
                .build();
    }
}
