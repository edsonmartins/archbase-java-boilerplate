package br.com.archbase.boilerplate.rest.infrastructure.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Classe padrão para respostas de erro da API.
 * Segue o padrão dos projetos vendax-promoter-api e gestor-rq-api.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private HttpStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime timestamp;

    private String message;
    private String debugMessage;
    private String path;
    private List<ApiSubError> subErrors;

    private ApiError() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiError(HttpStatus status) {
        this();
        this.status = status;
    }

    public ApiError(HttpStatus status, Throwable ex) {
        this();
        this.status = status;
        this.message = "Erro inesperado";
        this.debugMessage = ex.getLocalizedMessage();
    }

    public ApiError(HttpStatus status, String message, Throwable ex) {
        this();
        this.status = status;
        this.message = message;
        this.debugMessage = ex.getLocalizedMessage();
    }

    public ApiError(HttpStatus status, String message) {
        this();
        this.status = status;
        this.message = message;
    }

    private void addSubError(ApiSubError subError) {
        if (subErrors == null) {
            subErrors = new ArrayList<>();
        }
        subErrors.add(subError);
    }

    private void addValidationError(String object, String field, Object rejectedValue, String message) {
        addSubError(new ApiValidationError(object, field, rejectedValue, message));
    }

    private void addValidationError(String object, String message) {
        addSubError(new ApiValidationError(object, message));
    }

    private void addValidationError(FieldError fieldError) {
        this.addValidationError(
                fieldError.getObjectName(),
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage()
        );
    }

    public void addValidationFieldErrors(List<FieldError> fieldErrors) {
        fieldErrors.forEach(this::addValidationError);
    }

    private void addValidationError(ObjectError objectError) {
        this.addValidationError(
                objectError.getObjectName(),
                objectError.getDefaultMessage()
        );
    }

    public void addValidationObjectErrors(List<ObjectError> globalErrors) {
        globalErrors.forEach(this::addValidationError);
    }

    /**
     * Nome do campo que violou a restrição — o último nó do caminho.
     *
     * <p>Antes era um cast para {@code PathImpl}, do pacote {@code internal} do
     * Hibernate Validator. Depender de `internal` é contrato que ninguém prometeu
     * manter: a classe muda de lugar entre versões e o projeto para de compilar
     * numa atualização que deveria ser transparente.
     *
     * <p>{@code Path.Node#toString()} já rende `itens[2]` e `mapa[chave]` — usar
     * só `getName()` devolveria `itens`, e uma violação no terceiro elemento
     * ficaria indistinguível de uma no primeiro.
     */
    private static String nomeDoCampo(ConstraintViolation<?> cv) {
        jakarta.validation.Path.Node folha = null;
        for (jakarta.validation.Path.Node no : cv.getPropertyPath()) {
            folha = no;
        }
        return folha == null ? "" : folha.toString();
    }

    private void addValidationError(ConstraintViolation<?> cv) {
        this.addValidationError(
                cv.getRootBeanClass().getSimpleName(),
                leafNodeName(cv.getPropertyPath()),
                cv.getInvalidValue(),
                cv.getMessage()
        );
    }

    /** Nome do nó-folha do caminho da violação (substitui o cast p/ PathImpl interno do Hibernate Validator). */
    private static String leafNodeName(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name;
    }

    public void addValidationErrors(Set<ConstraintViolation<?>> constraintViolations) {
        constraintViolations.forEach(this::addValidationError);
    }
}
