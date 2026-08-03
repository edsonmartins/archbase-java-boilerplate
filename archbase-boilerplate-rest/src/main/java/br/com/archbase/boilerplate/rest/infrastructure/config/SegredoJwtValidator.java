package br.com.archbase.boilerplate.rest.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Confere o segredo do JWT na partida.
 *
 * <p><b>Por que isto existe.</b> O padrão
 * {@code ARCHBASE_JWT_SECRET:change-this-secret-key-in-production} <b>não é
 * Base64 válido</b> — tem hífen. O Archbase decodifica o segredo para montar a
 * chave HMAC, então a emissão do token quebra.
 *
 * <p>O efeito é traiçoeiro: a aplicação sobe sem reclamar, a autenticação valida
 * a senha corretamente, e só a emissão falha. O usuário vê "Erro interno do
 * servidor", HTTP 500, indistinguível de senha errada para quem está testando —
 * e o desenvolvedor vai investigar hash, tenant e mapeamento, tudo em ordem.
 *
 * <p>Falhar na partida, com o motivo escrito e o comando de correção pronto,
 * troca uma tarde de investigação por uma linha de log.
 *
 * <p>A validação <b>não</b> exige segredo forte em desenvolvimento — exige que
 * seja utilizável. Recusar chave curta demais para HS256 é o mínimo: uma chave de
 * 128 bits faria a biblioteca lançar em tempo de emissão, de novo tarde.
 */
@Component
public class SegredoJwtValidator {

    private static final Logger log = LoggerFactory.getLogger(SegredoJwtValidator.class);

    /** HS256 exige chave de ao menos 256 bits (RFC 7518, §3.2). */
    private static final int MINIMO_DE_BYTES = 32;

    private static final String PLACEHOLDER = "change-this-secret-key-in-production";

    private final String segredo;

    public SegredoJwtValidator(
            @Value("${archbase.security.jwt.secret-key:}") String segredo) {
        this.segredo = segredo;
    }

    @PostConstruct
    void conferir() {
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException(mensagem("ARCHBASE_JWT_SECRET não está definido."));
        }

        if (PLACEHOLDER.equals(segredo)) {
            throw new IllegalStateException(mensagem(
                    "ARCHBASE_JWT_SECRET ainda é o texto de exemplo do boilerplate, "
                            + "que não é Base64 válido."));
        }

        byte[] chave;
        try {
            chave = Base64.getDecoder().decode(segredo);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(mensagem(
                    "ARCHBASE_JWT_SECRET não é Base64 válido: " + e.getMessage()));
        }

        if (chave.length < MINIMO_DE_BYTES) {
            throw new IllegalStateException(mensagem(
                    "ARCHBASE_JWT_SECRET decodifica para " + chave.length
                            + " bytes; HS256 exige ao menos " + MINIMO_DE_BYTES + "."));
        }

        log.info("Segredo do JWT válido: {} bytes de chave.", chave.length);
    }

    private static String mensagem(String problema) {
        return problema + """


                Sem um segredo utilizável, a autenticação valida a senha e a emissão do
                token falha depois, devolvendo HTTP 500 sem pista da causa.

                Gere um e ponha no .env:

                    echo "ARCHBASE_JWT_SECRET=$(openssl rand -base64 48)" >> .env
                """;
    }
}
