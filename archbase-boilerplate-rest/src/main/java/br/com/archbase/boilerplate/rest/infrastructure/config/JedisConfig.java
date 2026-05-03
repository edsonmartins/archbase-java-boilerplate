package br.com.archbase.boilerplate.rest.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Configuração do Jedis (cliente Redis).
 *
 * <p>Configura o pool de conexões com o Redis para cache distribuído.</p>
 */
@Configuration
public class JedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        if (redisPassword != null && !redisPassword.isBlank()) {
            return new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword);
        }
        return new JedisPool(poolConfig, redisHost, redisPort);
    }
}
