package com.fabric.batch.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Local JWT Token Service for Development
 * 
 * Provides a mock implementation of JWT token service for local development.
 * This avoids dependency injection failures when JWT components are not available.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-19
 */
@Service
@Profile("local")
@Slf4j
public class LocalJwtTokenService implements JwtTokenServiceInterface {
    
    @Override
    public String generateToken(String username) {
        // Generate a simple token for local development
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 8);
        return "local_" + username + "_" + timestamp + "_" + random;
    }
    
    @Override
    public String extractUsername(String token) {
        if (token != null && token.startsWith("local_")) {
            String[] parts = token.split("_");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return "anonymous";
    }
    
    @Override
    public boolean validateToken(String token, String username) {
        // For local development, accept any token that contains the username
        return token != null && token.contains(username);
    }
    
    @Override
    public boolean isTokenExpired(String token) {
        // For local development, tokens never expire
        return false;
    }
    
    @Override
    public String refreshToken(String token) {
        String username = extractUsername(token);
        return generateToken(username);
    }
}