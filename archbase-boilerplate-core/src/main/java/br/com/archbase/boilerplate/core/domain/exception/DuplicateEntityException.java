package br.com.archbase.boilerplate.core.domain.exception;

/**
 * Exceção lançada quando se tenta criar uma entidade duplicada.
 */
public class DuplicateEntityException extends BoilerplateException {

    private final String entityName;
    private final String field;
    private final String value;

    public DuplicateEntityException(String entityName, String field, String value) {
        super(String.format("%s já existe com %s: %s", entityName, field, value));
        this.entityName = entityName;
        this.field = field;
        this.value = value;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
