package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Hidden
@RestControllerAdvice
public class PreRegistrationExceptionHandler {

    @ExceptionHandler(PreRegistrationException.class)
    public ResponseEntity<Map<String, String>> handlePreRegistrationException(PreRegistrationException ex) {
        String msg = ex.getMessage();
        HttpStatus status = msg != null && msg.contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", msg != null ? msg : "Pre-registration error"));
    }
}
