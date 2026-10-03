package br.com.archbase.boilerplate.rest.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuração de Rate Limiting com Bucket4j, aplicada pelo {@code RateLimitingFilter}.
 *
 * <p>Três perfis de limite, cada um com seu próprio conjunto de buckets:
 * <ul>
 *   <li>webhooks ({@code /api/v1/webhooks/**}, por IP): 100 req/min</li>
 *   <li>usuários autenticados (por usuário): 1000 req/min</li>
 *   <li>requisições anônimas (por IP): 50 req/min</li>
 * </ul>
 * Os valores vêm de {@code security.rate-limit.*}; {@code security.rate-limit.enabled=false}
 * desliga o filtro.
 *
 * <p>Os buckets ficam em caches Caffeine com expiração e teto de tamanho: um mapa simples cresceria
 * sem limite, uma entrada por IP, e o próprio rate limit viraria vetor de esgotamento de memória.
 * O estado é local à instância — com várias réplicas o limite efetivo é multiplicado por elas.
 */
@Configuration
public class RateLimitingConfig {

    private static final long MAX_BUCKETS = 100_000;

    private final boolean enabled;
    private final int webhookLimit;
    private final int webhookDurationSeconds;
    private final int authenticatedLimit;
    private final int authenticatedDurationSeconds;
    private final int ipLimit;
    private final int ipDurationSeconds;

    private final Cache<String, Bucket> webhookBuckets;
    private final Cache<String, Bucket> authenticatedBuckets;
    private final Cache<String, Bucket> ipBuckets;

    public RateLimitingConfig(
            @Value("${security.rate-limit.enabled:true}") boolean enabled,
            @Value("${security.rate-limit.webhook.limit:100}") int webhookLimit,
            @Value("${security.rate-limit.webhook.duration:60}") int webhookDurationSeconds,
            @Value("${security.rate-limit.authenticated.limit:1000}") int authenticatedLimit,
            @Value("${security.rate-limit.authenticated.duration:60}") int authenticatedDurationSeconds,
            @Value("${security.rate-limit.ip.limit:50}") int ipLimit,
            @Value("${security.rate-limit.ip.duration:60}") int ipDurationSeconds) {
        this.enabled = enabled;
        this.webhookLimit = webhookLimit;
        this.webhookDurationSeconds = webhookDurationSeconds;
        this.authenticatedLimit = authenticatedLimit;
        this.authenticatedDurationSeconds = authenticatedDurationSeconds;
        this.ipLimit = ipLimit;
        this.ipDurationSeconds = ipDurationSeconds;
        // Um bucket ocioso por mais que a janela já está cheio de novo, então descartá-lo é inócuo.
        this.webhookBuckets = novoCache(webhookDurationSeconds);
        this.authenticatedBuckets = novoCache(authenticatedDurationSeconds);
        this.ipBuckets = novoCache(ipDurationSeconds);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Bucket de webhooks para a chave (IP de origem). */
    public Bucket webhookBucket(String key) {
        return webhookBuckets.get(key, k -> createNewWebhookBucket());
    }

    /** Bucket de usuário autenticado para a chave (nome do usuário). */
    public Bucket authenticatedBucket(String key) {
        return authenticatedBuckets.get(key, k -> createNewAuthenticatedBucket());
    }

    /** Bucket de requisição anônima para a chave (IP de origem). */
    public Bucket ipBucket(String key) {
        return ipBuckets.get(key, k -> createNewIpBucket());
    }

    public Bucket createNewWebhookBucket() {
        return novoBucket(webhookLimit, webhookDurationSeconds);
    }

    public Bucket createNewAuthenticatedBucket() {
        return novoBucket(authenticatedLimit, authenticatedDurationSeconds);
    }

    public Bucket createNewIpBucket() {
        return novoBucket(ipLimit, ipDurationSeconds);
    }

    private static Bucket novoBucket(int limit, int durationSeconds) {
        Bandwidth bandwidth = Bandwidth.classic(limit,
                Refill.intervally(limit, Duration.ofSeconds(durationSeconds)));
        return Bucket4j.builder().addLimit(bandwidth).build();
    }

    private static Cache<String, Bucket> novoCache(int durationSeconds) {
        return Caffeine.newBuilder()
                .maximumSize(MAX_BUCKETS)
                .expireAfterAccess(Duration.ofSeconds(durationSeconds).multipliedBy(2))
                .build();
    }
}
