package store.auth;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    public record ErrorOut(String timestamp, int status, String error, String message, String path) {}

    // JSON malformado ou tipo errado (ex.: "name":{} ou corpo vazio)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorOut> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or invalid request body", req);
    }

    // ResponseStatusException (400/401/404/409 do Service/Resource) e erros do proprio Spring MVC
    // (405, 404 de rota inexistente, 415...) implementam ErrorResponse e trazem status + mensagem.
    // Qualquer outra excecao e um erro inesperado: vira 500 sem expor detalhes internos.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorOut> handle(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse er) {
            // mantem headers que o Spring ja preparou (ex.: Allow no 405)
            return ResponseEntity.status(er.getStatusCode())
                .headers(er.getHeaders())
                .body(build(er.getStatusCode(), er.getBody().getDetail(), req).getBody());
        }
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req);
    }

    private ResponseEntity<ErrorOut> build(HttpStatusCode status, String message, HttpServletRequest req) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        ErrorOut body = new ErrorOut(
            Instant.now().toString(),
            status.value(),
            resolved != null ? resolved.getReasonPhrase() : "Error",
            message,
            req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

}
