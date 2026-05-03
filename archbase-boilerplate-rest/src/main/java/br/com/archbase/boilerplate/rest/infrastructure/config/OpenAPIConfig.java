package br.com.archbase.boilerplate.rest.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuração do OpenAPI/Swagger para documentação da API.
 */
@Configuration
public class OpenAPIConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${openapi.server.dev.url:http://localhost:8080}")
    private String devUrl;

    @Value("${openapi.server.dev.description:Development server}")
    private String devDescription;

    @Value("${openapi.server.homolog.url:}")
    private String homologUrl;

    @Value("${openapi.server.homolog.description:Homologation server}")
    private String homologDescription;

    @Value("${openapi.server.prod.url:}")
    private String prodUrl;

    @Value("${openapi.server.prod.description:Production server}")
    private String prodDescription;

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
                .servers(buildServers())
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Authorization header using Bearer scheme. Example: \"Authorization: Bearer {token}\"")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    private List<Server> buildServers() {
        List<Server> servers = new ArrayList<>();

        servers.add(new Server()
                .url(devUrl)
                .description(devDescription));

        if (homologUrl != null && !homologUrl.isBlank()) {
            servers.add(new Server()
                    .url(homologUrl)
                    .description(homologDescription));
        }

        if (prodUrl != null && !prodUrl.isBlank()) {
            servers.add(new Server()
                    .url(prodUrl)
                    .description(prodDescription));
        }

        return servers;
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
