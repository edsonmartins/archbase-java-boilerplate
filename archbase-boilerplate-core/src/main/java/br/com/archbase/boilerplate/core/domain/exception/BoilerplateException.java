package br.com.archbase.boilerplate.core.domain.exception;

/**
 * Exceção base para todas as exceções de negócio do sistema.
 */
public class BoilerplateException extends RuntimeException {

    public BoilerplateException(String message) {
        super(message);
    }

    public BoilerplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
