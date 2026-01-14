package com.ccc.okrtracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response structure for all API exceptions.
 * Provides consistent error format for frontend consumption.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Omit null fields from JSON response
public class ErrorResponse {
    
    private int status;
    
    private String error;
    
    private String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String path;
    
    private Map<String, String> validationErrors; // For validation failures
    
    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }
    
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }
}
