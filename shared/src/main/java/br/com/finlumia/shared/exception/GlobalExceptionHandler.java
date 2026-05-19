package br.com.finlumia.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.finlumia.shared.views.DialogDefault;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FinlumiaException.class)
    public ResponseEntity<DialogDefault> handleFinlumiaException(FinlumiaException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        DialogDefault body = new DialogDefault(
                exception.getCode(),
                exception.getTitle(),
                exception.getMessage());

        return ResponseEntity.status(status).body(body);
    }
}
