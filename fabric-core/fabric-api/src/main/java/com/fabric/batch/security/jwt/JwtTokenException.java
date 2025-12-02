package com.fabric.batch.security.jwt;

/**
 * JWT Token Exception
 * 
 * Custom exception for JWT token processing errors including generation,
 * validation, parsing, and security violations. Provides specific error
 * handling for authentication and authorization failures.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
public class JwtTokenException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new JWT token exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public JwtTokenException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new JWT token exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     */
    public JwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new JWT token exception with the specified cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     */
    public JwtTokenException(Throwable cause) {
        super(cause);
    }
}