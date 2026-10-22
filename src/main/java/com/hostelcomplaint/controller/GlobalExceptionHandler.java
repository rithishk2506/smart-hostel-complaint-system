package com.hostelcomplaint.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Method-level authorization failures must be reported as 403, not 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException exception) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailure(
            AuthenticationException exception) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource.");
    }

    // Preserve the HTTP status explicitly requested by ResponseStatusException.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException exception) {
        int value = exception.getStatusCode().value();
        HttpStatus status = HttpStatus.resolve(value);
        String error = status == null ? "Error" : status.getReasonPhrase();
        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = error;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", value);
        body.put("error", error);
        body.put("message", message);

        return ResponseEntity.status(exception.getStatusCode()).body(body);
    }

    // Handle requests that violate business rules.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    // Handle invalid input.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    // Handle missing resources and other runtime errors.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null
                && message.toLowerCase().contains("not found")) {
            return buildResponse(HttpStatus.NOT_FOUND, message);
        }

        log.error("Unhandled runtime exception", exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred.");
    }

    // Handle unexpected exceptions without exposing internal details.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception exception) {
        log.error("Unhandled application exception", exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
