# 01. Estrutura de Projeto

Estrutura hexagonal do boilerplate: Spring Boot 4.1.0, Java 25, Archbase 3.2.2, Maven multi-módulo.
O CRUD de `Produto` é a referência de código (o que o repositório faz hoje).

---

## Estrutura de Diretórios

```
archbase-java-boilerplate/                       # pom pai (br.com.archbase.boilerplate:archbase-java-boilerplate)
├── pom.xml
├── Makefile  docker-compose.yml  .env.example
│
├── archbase-boilerplate-core/
│   └── src/main/java/br/com/archbase/boilerplate/core/
│       ├── domain/
│       │   ├── entity/                          # Produto (domínio)
│       │   ├── enums/                           # CategoriaProduto
│       │   └── exception/                       # BoilerplateException e filhas
│       ├── application/
│       │   ├── port/out/                        # ProdutoPersistencePort
│       │   ├── service/                         # ProdutoService, security/*
│       │   └── dto/                             # ProdutoDTO, ProdutoCreateDTO, ProdutoUpdateDTO, ProdutoEstatisticasDTO
│       └── infrastructure/output/persistence/
│           ├── entity/                          # ProdutoEntity (JPA) + QProdutoEntity (gerada)
│           ├── repository/                      # ProdutoJpaRepository
│           ├── adapter/                         # ProdutoPersistenceAdapter
│           └── mapper/                          # ProdutoPersistenceMapper (MapStruct)
│
└── archbase-boilerplate-rest/
    ├── src/main/java/br/com/archbase/boilerplate/rest/
    │   ├── ArchbaseBoilerplateApplication.java  # Main class
    │   ├── infrastructure/
    │   │   ├── input/rest/                      # ProdutoController
    │   │   ├── config/                          # QueryDslConfig, OpenAPIConfig, JacksonConfig, RateLimitingConfig...  (+ filter/RateLimitingFilter)
    │   │   ├── error/                           # RestExceptionHandler, ApiError
    │   │   └── filter/                          # TenantContextFilter, TenantAccessValidationFilter
    │   └── seed/                                # AdminSeedLoader, DataSeedLoader
    └── src/main/resources/
        ├── application.yml  application-{dev,h2,homolog,prod}.yml
        ├── db/migration/V1__schema_inicial.sql  # Flyway
        └── ehcache.xml  logback-spring.xml
```

Observação: hoje não existe `port/in` (use cases). O `ProdutoController` chama `ProdutoService`, que usa
`ProdutoJpaRepository` direto e converte com `ProdutoEntity.toDTO()` / `fromDTO()`. O trio
`ProdutoPersistencePort` + `ProdutoPersistenceAdapter` + `ProdutoPersistenceMapper` (domínio `Produto` <-> entity)
existe e compila, mas nenhuma classe do repositório o injeta além do próprio adapter.

---

## pom.xml (Parent) — trechos reais

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
    <relativePath/>
</parent>

<groupId>br.com.archbase.boilerplate</groupId>
<artifactId>archbase-java-boilerplate</artifactId>
<version>1.0.0</version>
<packaging>pom</packaging>

<modules>
    <module>archbase-boilerplate-core</module>
    <module>archbase-boilerplate-rest</module>
</modules>

<properties>
    <java.version>25</java.version>
    <archbase.version>3.2.2</archbase.version>
    <org.mapstruct.version>1.6.3</org.mapstruct.version>
    <!-- QueryDSL: fork openfeign (Jakarta). NÃO sobrescrever querydsl.version
         (o spring-boot-dependencies usa para o querydsl-bom). -->
    <openfeign-querydsl.version>7.2</openfeign-querydsl.version>
    <postgresql.version>42.7.8</postgresql.version>
    <h2.version>2.2.224</h2.version>
    <springdoc.version>3.0.0</springdoc.version>
    <jjwt.version>0.13.0</jjwt.version>
    <caffeine.version>3.1.8</caffeine.version>
    <lombok.version>1.18.46</lombok.version>
    <langchain4j.version>1.17.0</langchain4j.version>
</properties>
```

No `dependencyManagement` do pai: `archbase-starter`, `archbase-starter-flyway`, postgresql, h2,
`io.github.openfeign.querydsl:querydsl-jpa` e `querydsl-apt` (classifier `jakarta`, scope `provided`),
springdoc, jjwt, caffeine, mapstruct e langchain4j.

---

## pom.xml (Core) — trechos reais

```xml
<dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>

    <dependency><groupId>br.com.archbase</groupId><artifactId>archbase-starter</artifactId></dependency>

    <!-- Flyway: o schema é versionado (prod usa ddl-auto: validate) -->
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <dependency><groupId>br.com.archbase</groupId><artifactId>archbase-starter-flyway</artifactId></dependency>

    <!-- QueryDSL (fork openfeign) -->
    <dependency>
        <groupId>io.github.openfeign.querydsl</groupId>
        <artifactId>querydsl-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.openfeign.querydsl</groupId>
        <artifactId>querydsl-apt</artifactId>
        <classifier>jakarta</classifier>
        <scope>provided</scope>
    </dependency>

    <dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    <!-- também: postgresql (runtime), h2 (test), caffeine, ehcache (jakarta), hibernate-jcache,
         spring-boot-starter-test e junit-platform-launcher (test) -->
</dependencies>

<build>
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <release>${java.version}</release>
                <!-- Java 23+ não roda annotation processing implicitamente -->
                <proc>full</proc>
                <annotationProcessorPaths>
                    <path>lombok ${lombok.version}</path>
                    <path>lombok-mapstruct-binding 0.2.0</path>
                    <path>mapstruct-processor ${org.mapstruct.version}</path>
                    <path>
                        <groupId>io.github.openfeign.querydsl</groupId>
                        <artifactId>querydsl-apt</artifactId>
                        <version>${openfeign-querydsl.version}</version>
                        <classifier>jakarta</classifier>
                    </path>
                    <path>jakarta.persistence-api 3.1.0</path>
                </annotationProcessorPaths>
                <compilerArgs>
                    <arg>-Amapstruct.suppressGeneratorTimestamp=true</arg>
                    <arg>-Amapstruct.defaultComponentModel=spring</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

(`<path>nome versão</path>` acima é abreviação; no pom cada `path` tem `groupId`/`artifactId`/`version`.)

## pom.xml (REST)

Depende de `archbase-boilerplate-core` e declara: `spring-boot-starter-web` (com o Tomcat excluído) +
`spring-boot-starter-jetty` + `org.eclipse.jetty.http2:jetty-http2-server` (obrigatório com
`server.http2.enabled: true`), `starter-actuator`, `starter-security`, `starter-validation`,
`starter-webflux`, `starter-hateoas` e `starter-data-rest` (o springdoc/Archbase exigem as autoconfigurações;
sem eles a aplicação não sobe), `springdoc-openapi-starter-webmvc-ui`, `jedis`, `bucket4j-core` 7.6.0 e `h2`
(runtime). Plugins: `spring-boot-maven-plugin` e `maven-failsafe-plugin` (testes `*IT` só rodam com
`mvn verify`).

---

## application.yml (principais blocos reais)

```yaml
spring:
  profiles:
    active: ${APP_PROFILE:dev}
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DATABASE:archbase_db}
    username: ${POSTGRES_USER:archbase}
    password: ${POSTGRES_PASSWORD:changeit}
  jpa:
    hibernate:
      ddl-auto: update            # prod: validate
    open-in-view: true            # o archbase-security depende do OSIV
  data:
    rest:
      detection-strategy: annotated   # não publicar CRUD HTTP para todo repositório

archbase:
  multitenancy:
    enabled: true
  app:
    tenant:
      default:
        id: ${ARCHBASE_DEFAULT_TENANT_ID:a9f814d2-4dae-41f3-851b-8aa3d4706561}
      accept-query-param: false
      fail-on-missing: false
    jpa:
      repositories: br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository
      entities: br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity
    component:
      scan: br.com.archbase.boilerplate
  security:
    jwt:
      secret-key: ${ARCHBASE_JWT_SECRET:change-this-secret-key-in-production}
      token-expiration: 86400000
      refresh-expiration: 604800000
      strict-token-use: true
    scan-packages: br.com.archbase.boilerplate.rest.infrastructure.input.rest
    whitelist: /actuator/health,/swagger-ui/**,/v3/api-docs/**,/api/v1/public/**
    # endurecimento ligado: prevent-user-enumeration, password.*, admin-guard,
    # hardening.validation: fail, public-paths.registration: false ...
```

O bloco `archbase.security` completo (política de senha com `min-length: 12`, `admin-endpoints.policy: permit`,
CORS etc.) está comentado no próprio `application.yml` — leia lá antes de afrouxar qualquer flag.

Perfis: `dev` (padrão; Flyway ligado, `ddl-auto: update`), `h2` (banco em memória, escolha consciente),
`homolog` e `prod` (`ddl-auto: validate`, Flyway dono do schema).

## Migrations (Flyway)

`archbase-boilerplate-rest/src/main/resources/db/migration/V1__schema_inicial.sql` cria `produto` e as tabelas de
segurança do Archbase. É gerado a partir do mapeamento (pg_dump de um banco criado com `ddl-auto=create`), não
escrito à mão. Entidade nova: gere/escreva a migration e confira localmente com `ddl-auto: validate`.

---

## Main Application Class

```java
@SpringBootApplication
@ComponentScan(basePackages = {"br.com.archbase.boilerplate"})
@EntityScan(basePackages = {
    "br.com.archbase.boilerplate.core.infrastructure.output.persistence.entity",
    "br.com.archbase.ddd.domain.entity"
})   // import org.springframework.boot.persistence.autoconfigure.EntityScan (Boot 4)
@EnableJpaRepositories(
    basePackages = {"br.com.archbase.boilerplate.core.infrastructure.output.persistence.repository"},
    repositoryBaseClass = CommonArchbaseJpaRepository.class
)
@EnableTransactionManagement
public class ArchbaseBoilerplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArchbaseBoilerplateApplication.class, args);
    }
}
```

`CommonArchbaseJpaRepository` vem de `br.com.archbase.ddd.infraestructure.persistence.jpa.repository`.
O `JPAQueryFactory` usado pelos adapters é fornecido por `QueryDslConfig` (módulo rest).

---

## Comandos Úteis

```bash
# Infra local (PostgreSQL, Redis, Adminer, RedisInsight)
make docker-up

# Compilar (gera as classes Q do QueryDSL em target/generated-sources)
mvn clean compile

# Empacotar sem testes
mvn clean package -DskipTests

# Testes: mvn test (sem infra); mvn verify também roda *IT (exigem banco)
mvn test

# Executar (profile dev é o padrão)
mvn spring-boot:run -pl archbase-boilerplate-rest

# Executar em H2, sem infraestrutura. A migration V1 é específica de PostgreSQL, então no H2 é preciso
# desligar o Flyway (o Hibernate cria o schema) e informar um ARCHBASE_JWT_SECRET em Base64 de >= 32 bytes
# (ex.: openssl rand -base64 48). Login do seed em dev: admin@archbase.com.br / admin.
SPRING_FLYWAY_ENABLED=false ARCHBASE_JWT_SECRET=$(openssl rand -base64 48) \
  mvn spring-boot:run -pl archbase-boilerplate-rest -Dspring-boot.run.profiles=h2
```

---

**IMPORTANTE**: Repositories estendem `ArchbaseCommonJpaRepository<Entity, String, Long>` e entidades multi-tenant estendem `TenantPersistenceEntityBase`.
