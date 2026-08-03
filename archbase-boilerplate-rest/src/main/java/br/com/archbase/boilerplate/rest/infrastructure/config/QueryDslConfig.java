package br.com.archbase.boilerplate.rest.infrastructure.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fábrica de consultas do QueryDSL.
 *
 * <p><b>Por que isto existe.</b> A convenção do Archbase é que repositório não
 * tem método próprio: consulta vive no adapter de persistência, escrita em
 * QueryDSL. O adapter injeta {@link JPAQueryFactory} — mas nenhum bean o
 * fornecia, e a aplicação não subia:
 *
 * <pre>NoSuchBeanDefinitionException: No qualifying bean of type 'com.querydsl.jpa.impl.JPAQueryFactory'</pre>
 *
 * <p>O erro aparece na partida, o que é bom, mas manda o desenvolvedor procurar
 * defeito no próprio código quando o que falta é infraestrutura do esqueleto.
 * Todo projeto que segue a convenção precisa deste bean; entregá-lo aqui evita
 * que cada um o redescubra.
 */
@Configuration
public class QueryDslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
