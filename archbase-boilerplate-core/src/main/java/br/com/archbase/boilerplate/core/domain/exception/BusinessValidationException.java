package br.com.archbase.boilerplate.core.domain.exception;

/**
 * Exceção lançada quando uma regra de negócio é violada.
 */
public class BusinessValidationException extends BoilerplateException {

    private final String field;

    public BusinessValidationException(String message) {
        super(message);
        this.field = null;
    }

    public BusinessValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
