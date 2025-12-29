# 01. Estrutura de Projeto

Estrutura hexagonal completa para projetos Spring Boot 3 com Archbase Framework.

---

## Estrutura de Diretórios

```
project-core/
├── src/main/java/
│   └── br/com/empresa/projeto/
│       ├── domain/                          # DOMÍNIO (puro, sem dependências)
│       │   ├── entity/                      # Domain Objects
│       │   └── enums/                       # Enums do domínio
│       ├── application/                     # APLICAÇÃO (use cases, ports)
│       │   ├── port/
│       │   │   ├── in/                      # Ports de entrada (use cases)
│       │   │   └── out/                     # Ports de saída (persistence, external)
│       │   ├── service/                     # Serviços que implementam ports
│       │   └── dto/                         # DTOs
│       └── infrastructure/                  # INFRAESTRUTURA
│           ├── output/
│           │   ├── persistence/
│           │   │   ├── entity/              # JPA Entities
│           │   │   ├── repository/          # JPA Repositories
│           │   │   └── adapter/             # Persistence Adapters
│           │   └── mapper/                  # MapStruct Mappers
│           └── config/                      # Configurações Spring
│
└── src/main/resources/
    ├── application.yml
    └── application-dev.yml
```

```
project-rest/
├── src/main/java/
│   └── br/com/empresa/projeto/
│       └── rest/
│           ├── infrastructure/
│           │   ├── input/
│           │   │   └── rest/                # Controllers REST
│           │   └── config/                  # Configurações REST
│           └── ProjectApplication.java      # Main class
└── src/main/resources/
    ├── application.yml
    └── application-dev.yml
```

---

## pom.xml (Parent)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
    </parent>

    <groupId>br.com.empresa.projeto</groupId>
    <artifactId>project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>project-core</module>
        <module>project-rest</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <archbase.version>2.0.0</archbase.version>
        <querydsl.version>5.1.0</querydsl.version>
        <org.mapstruct.version>1.5.5.Final</org.mapstruct.version>
        <lombok.version>1.18.34</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>br.com.archbase</groupId>
                <artifactId>archbase-starter</artifactId>
                <version>${archbase.version}</version>
            </dependency>
            <dependency>
                <groupId>com.querydsl</groupId>
                <artifactId>querydsl-jpa</artifactId>
                <classifier>jakarta</classifier>
                <version>${querydsl.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

---

## pom.xml (Core Module)

```xml
<dependencies>
    <!-- Archbase Framework -->
    <dependency>
        <groupId>br.com.archbase</groupId>
        <artifactId>archbase-starter</artifactId>
    </dependency>

    <!-- QueryDSL -->
    <dependency>
        <groupId>com.querydsl</groupId>
        <artifactId>querydsl-jpa</artifactId>
        <classifier>jakarta</classifier>
    </dependency>
    <dependency>
        <groupId>com.querydsl</groupId>
        <artifactId>querydsl-apt</artifactId>
        <classifier>jakarta</classifier>
        <scope>provided</scope>
    </dependency>

    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>com.querydsl</groupId>
                        <artifactId>querydsl-apt</artifactId>
                        <classifier>jakarta</classifier>
                        <version>${querydsl.version}</version>
                    </path>
                    <path>
                        <groupId>jakarta.persistence</groupId>
                        <artifactId>jakarta.persistence-api</artifactId>
                        <version>3.1.0</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${org.mapstruct.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## application.yml

```yaml
spring:
  application:
    name: projeto-api
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/projeto}
    username: ${DATABASE_USER:projeto}
    password: ${DATABASE_PASSWORD:changeit}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Archbase
archbase:
  multitenancy:
    enabled: true
    default-tenant-id: default-tenant
  security:
    jwt:
      secret-key: ${JWT_SECRET:change-this-secret-key}
      token-expiration: 86400000
    scan-packages: br.com.empresa.projeto.rest.infrastructure.input.rest
    whitelist: /actuator/health,/swagger-ui/**,/v3/api-docs/**

# OpenAPI
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

---

## Main Application Class

```java
@SpringBootApplication
@ComponentScan(basePackages = {"br.com.empresa.projeto"})
@EntityScan(basePackages = {
    "br.com.empresa.projeto.core.infrastructure.output.persistence.entity",
    "br.com.archbase.ddd.domain.entity"
})
@EnableJpaRepositories(
    basePackages = {"br.com.empresa.projeto.core.infrastructure.output.persistence.repository"},
    repositoryBaseClass = CommonArchbaseJpaRepository.class
)
@EnableTransactionManagement
public class ProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjectApplication.class, args);
    }
}
```

---

## Comandos Maven Úteis

```bash
# Compilar projeto
mvn clean compile

# Gerar classes Q (QueryDSL)
mvn clean process-sources

# Empacotar sem testes
mvn clean package -DskipTests

# Executar com profile dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Executar JAR
java -jar project-rest/target/project-rest-1.0.0.jar --spring.profiles.active=dev
```

---

**IMPORTANTE**: Sempre use `ArchbaseCommonJpaRepository` como base para repositories e `TenantPersistenceEntityBase` para entidades multi-tenant.
