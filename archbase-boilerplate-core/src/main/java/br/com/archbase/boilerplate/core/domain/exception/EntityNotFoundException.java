package br.com.archbase.boilerplate.core.domain.exception;

/**
 * Exceção lançada quando uma entidade não é encontrada.
 */
public class EntityNotFoundException extends BoilerplateException {

    private final String entityName;
    private final String identifier;

    public EntityNotFoundException(String entityName, String identifier) {
        super(String.format("%s não encontrado(a): %s", entityName, identifier));
        this.entityName = entityName;
        this.identifier = identifier;
    }

    public EntityNotFoundException(String entityName, String fieldName, String identifier) {
        super(String.format("%s não encontrado(a) com %s: %s", entityName, fieldName, identifier));
        this.entityName = entityName;
        this.identifier = identifier;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getIdentifier() {
        return identifier;
    }
}
