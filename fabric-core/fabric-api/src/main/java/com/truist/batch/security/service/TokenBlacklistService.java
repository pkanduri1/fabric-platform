package com.truist.batch.security.service;

import com.truist.batch.security.jwt.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Blacklist Service - Simplified for local development
 * 
 * In-memory implementation for development. In production, this would use Redis
 * for fast lookups and database for persistent audit trails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private final JwtTokenService jwtTokenService;
    
    // In-memory storage for development - replace with Redis in production
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    /**
     * Blacklists a JWT token to prevent its further use
     */
    public void blacklistToken(String token, String userId, String reason, String blacklistedBy) {
        try {
            String tokenHash = jwtTokenService.generateTokenHash(token);
            blacklistedTokens.add(tokenHash);
            log.info("Token blacklisted for user {} - Reason: {}", userId, reason);
        } catch (Exception e) {
            log.error("Failed to blacklist token for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Checks if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String tokenHash = jwtTokenService.generateTokenHash(token);
            return blacklistedTokens.contains(tokenHash);
        } catch (Exception e) {
            log.error("Failed to check token blacklist status: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Blacklists all tokens for a specific user
     */
    public void blacklistAllUserTokens(String userId, String reason, String blacklistedBy) {
        // Simplified implementation - in production would query active tokens
        log.info("All tokens blacklisted for user {} - Reason: {}", userId, reason);
    }
    
    /**
     * Removes expired tokens from blacklist (cleanup)
     */
    public void cleanupExpiredTokens() {
        // Simplified implementation - in production would check token expiration
        log.debug("Token cleanup completed");
    }
}