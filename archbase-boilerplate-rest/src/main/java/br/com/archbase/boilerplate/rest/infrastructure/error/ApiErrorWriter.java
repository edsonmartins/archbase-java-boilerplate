package br.com.archbase.boilerplate.rest.infrastructure.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escreve um {@link ApiError} direto na resposta, para quem responde fora do Spring MVC — filtros
 * servlet nunca chegam ao {@code RestExceptionHandler}, e um {@code sendError} devolveria a página
 * de erro do container em vez do JSON padrão da API.
 */
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message) throws IOException {
        ApiError apiError = new ApiError(status, message);
        // Só o caminho: a query string pode carregar token ou dado pessoal.
        apiError.setPath(request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), apiError);
    }
}
