package com.ccc.okrtracker.config;

import com.ccc.okrtracker.dto.ErrorResponse;
import com.ccc.okrtracker.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across the entire application.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ResourceNotFoundException (404 errors).
     * Thrown when a requested entity is not found in the database.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, 
            WebRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle AccessDeniedException (403 errors).
     * Thrown when a user attempts to access a resource without proper permissions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, 
            WebRequest request) {
        
        log.warn("Access denied: {} for path: {}", ex.getMessage(), getRequestPath(request));
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "You do not have permission to access this resource",
            getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle AuthenticationException (401 errors).
     * Thrown when JWT token is invalid, expired, or missing.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, 
            WebRequest request) {
        
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            "Authentication failed. Please log in again.",
            getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle InsufficientAuthenticationException (401 errors).
     * Thrown when authentication is required but not provided.
     */
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientAuthenticationException(
            InsufficientAuthenticationException ex, 
            WebRequest request) {
        
        log.warn("Insufficient authentication: {} for path: {}", ex.getMessage(), getRequestPath(request));
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            "Authentication is required to access this resource.",
            getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle validation errors (400 errors).
     * Triggered when @Valid annotation fails on request body.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, 
            WebRequest request) {
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed for path: {} with errors: {}", getRequestPath(request), validationErrors);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Validation failed",
            getRequestPath(request)
        );
        error.setValidationErrors(validationErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle IllegalArgumentException (400 errors).
     * Thrown when invalid arguments are passed to methods.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            WebRequest request) {
        
        log.warn("Invalid argument: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle ConstraintViolationException (400 errors).
     * Triggered by JPA/Hibernate validation constraints.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, 
            WebRequest request) {
        
        Map<String, String> validationErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        log.warn("Constraint violation for path: {} with errors: {}", getRequestPath(request), validationErrors);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Constraint violation occurred.",
            getRequestPath(request)
        );
        error.setValidationErrors(validationErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle MethodArgumentTypeMismatchException (400 errors).
     * Thrown when path variables or request parameters have incorrect types.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, 
            WebRequest request) {
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        log.warn("Type mismatch: {}", message);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle IllegalStateException (409 errors).
     * Thrown when an operation is attempted in an invalid state.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, 
            WebRequest request) {
        
        log.error("Illegal state: {} for path: {}", ex.getMessage(), getRequestPath(request));
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle file upload size exceeded errors (413 errors).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, 
            WebRequest request) {
        
        log.warn("File upload size exceeded: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "Payload Too Large",
            "The uploaded file exceeds the maximum allowed size",
            getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    /**
     * Handle all other unhandled exceptions (500 errors).
     * Logs the full stack trace but returns a sanitized error message to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, 
            WebRequest request) {
        
        // Log full stack trace for debugging
        log.error("Unhandled exception occurred: ", ex);
        
        // Return sanitized error to client (don't expose internal details)
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Helper method to extract request path from WebRequest.
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return request.getDescription(false).replace("uri=", "");
    }
}
