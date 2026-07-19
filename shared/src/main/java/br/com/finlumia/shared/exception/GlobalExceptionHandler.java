package br.com.finlumia.shared.exception;

import java.util.stream.Collectors;

import br.com.finlumia.shared.views.DialogDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FinlumiaException.class)
    public ResponseEntity<DialogDefault> handleFinlumiaException(FinlumiaException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(
                new DialogDefault(exception.getCode(), exception.getTitle(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DialogDefault> handleValidation(MethodArgumentNotValidException exception) {
        String fields = exception.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                new DialogDefault(422, "Dados inválidos", fields));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<DialogDefault> handleRateLimit(RateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
                .body(new DialogDefault(429, exception.getTitle(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<DialogDefault> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String message = "Parâmetro '" + exception.getName() + "' inválido: valor '" + exception.getValue() + "' não é aceito.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DialogDefault(400, "Dados inválidos", message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<DialogDefault> handleMissingParam(MissingServletRequestParameterException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new DialogDefault(400, "Dados inválidos", "Parâmetro obrigatório ausente: " + exception.getParameterName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<DialogDefault> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new DialogDefault(404, "Recurso não encontrado", "Nenhum recurso disponível para esta rota."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DialogDefault> handleGeneric(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new DialogDefault(500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente."));
    }
}
