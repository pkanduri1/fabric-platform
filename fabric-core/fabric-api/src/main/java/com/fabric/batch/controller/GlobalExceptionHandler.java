package com.fabric.batch.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 *
 * Provides consistent JSON error responses for validation failures and
 * other common exceptions across all API endpoints.
 *
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Source System Validation Enhancement
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles bean validation errors (from @Valid on request bodies).
     * Formats all constraint violation messages, joined by semicolons, into a standardised error response.
     *
     * @param ex the validation exception
     * @return 400 Bad Request with JSON body containing error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        // Collect all field-level validation messages
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining("; "));

        log.warn("[{}] Validation failed: {}", correlationId, message);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "VALIDATION_ERROR");
        body.put("message", message);
        body.put("correlationId", correlationId);
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
