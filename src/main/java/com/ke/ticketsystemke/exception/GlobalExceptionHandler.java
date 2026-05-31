package com.ke.ticketsystemke.exception;

import com.ke.ticketsystemke.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        String cid = MDC.get("correlationId");
        log.warn("event=response_status_exception status={} reason={} correlationId={}", ex.getStatusCode(), ex.getReason(), cid);
        ErrorResponse body = new ErrorResponse(ex.getReason(), cid);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String cid = MDC.get("correlationId");
        String fields = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("event=validation_failure fields={} correlationId={}", fields, cid);
        ErrorResponse body = new ErrorResponse("Validation failed", cid);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        String cid = MDC.get("correlationId");
        log.warn("event=access_denied message={} correlationId={}", ex.getMessage(), cid);
        ErrorResponse body = new ErrorResponse("Access denied", cid);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        String cid = MDC.get("correlationId");
        log.error("event=unexpected_exception correlationId={}", cid, ex);
        ErrorResponse body = new ErrorResponse("An internal error occurred", cid);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
