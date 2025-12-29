package br.com.archbase.boilerplate.rest;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Classe principal da aplicação Archbase Boilerplate.
 * <p>
 * Nota: O framework Archbase já configura automaticamente seus próprios pacotes
 * através de ArchbaseComponentScanConfiguration. Portanto, aqui configuramos
 * apenas os pacotes específicos do boilerplate.
 * </p>
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "br.com.archbase.boilerplate"
})
@EntityScan(basePackages = {
        "br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity",
        "br.com.archbase.ddd.domain.entity"
})
@EnableJpaRepositories(
        basePackages = {
            "br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository"
        },
        repositoryBaseClass = CommonArchbaseJpaRepository.class
)
@EnableTransactionManagement
public class ArchbaseBoilerplateApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ArchbaseBoilerplateApplication.class);
        app.run(args);
    }
}
