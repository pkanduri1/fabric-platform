package com.truist.batch.security.jwt;

/**
 * JWT Token Service Interface
 * 
 * Defines the contract for JWT token operations to enable proper
 * dependency injection and testing across different environments.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-19
 */
public interface JwtTokenServiceInterface {
    
    /**
     * Generate a token for the given username
     * 
     * @param username The username to generate token for
     * @return Generated token string
     */
    String generateToken(String username);
    
    /**
     * Extract username from the given token
     * 
     * @param token The token to extract username from
     * @return Extracted username
     */
    String extractUsername(String token);
    
    /**
     * Validate token for the given username
     * 
     * @param token The token to validate
     * @param username The username to validate against
     * @return true if token is valid for the username
     */
    boolean validateToken(String token, String username);
    
    /**
     * Check if the token has expired
     * 
     * @param token The token to check
     * @return true if token is expired
     */
    boolean isTokenExpired(String token);
    
    /**
     * Refresh an existing token
     * 
     * @param token The token to refresh
     * @return New refreshed token
     */
    String refreshToken(String token);
}