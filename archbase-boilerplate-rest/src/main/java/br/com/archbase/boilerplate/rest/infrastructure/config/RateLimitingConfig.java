package br.com.archbase.boilerplate.rest.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuração de Rate Limiting com Bucket4j.
 *
 * <p>Implementa controle de taxa de requisições para diferentes tipos de endpoints:
 * <ul>
 *   <li>Webhooks: 100 req/min (evita sobrecarga de webhooks externos)</li>
 *   <li>APIs autenticadas: 1000 req/min (uso normal do sistema)</li>
 *   <li>APIs por IP: 50 req/min (proteção contra abuso)</li>
 * </ul>
 * </p>
 */
@Configuration
public class RateLimitingConfig {

    @Value("${security.rate-limit.webhook.limit:100}")
    private int webhookLimit;

    @Value("${security.rate-limit.webhook.duration:60}")
    private int webhookDurationSeconds;

    @Value("${security.rate-limit.authenticated.limit:1000}")
    private int authenticatedLimit;

    @Value("${security.rate-limit.authenticated.duration:60}")
    private int authenticatedDurationSeconds;

    @Value("${security.rate-limit.ip.limit:50}")
    private int ipLimit;

    @Value("${security.rate-limit.ip.duration:60}")
    private int ipDurationSeconds;

    @Getter
    private final Map<String, Bucket> webhookBuckets = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, Bucket> authenticatedBuckets = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    /**
     * Cria bucket para webhooks (100 req/min).
     */
    public Bucket createNewWebhookBucket() {
        Bandwidth limit = Bandwidth.classic(webhookLimit,
                Refill.intervally(webhookLimit, Duration.ofSeconds(webhookDurationSeconds)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    /**
     * Cria bucket para usuários autenticados (1000 req/min).
     */
    public Bucket createNewAuthenticatedBucket() {
        Bandwidth limit = Bandwidth.classic(authenticatedLimit,
                Refill.intervally(authenticatedLimit, Duration.ofSeconds(authenticatedDurationSeconds)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    /**
     * Cria bucket por IP (50 req/min - proteção contra abuso).
     */
    public Bucket createNewIpBucket() {
        Bandwidth limit = Bandwidth.classic(ipLimit,
                Refill.intervally(ipLimit, Duration.ofSeconds(ipDurationSeconds)));
        return Bucket4j.builder().addLimit(limit).build();
    }
}
