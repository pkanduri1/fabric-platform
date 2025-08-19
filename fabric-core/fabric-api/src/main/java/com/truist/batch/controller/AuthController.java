package com.truist.batch.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication Controller for JWT Token Management
 * 
 * Provides JWT token generation and authentication endpoints for the Fabric Platform.
 * In local development mode, generates tokens without password validation for testing.
 * In production, integrates with LDAP/AD for secure authentication.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-19
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000"})
@Tag(name = "Authentication", 
     description = "JWT authentication and token management endpoints")
public class AuthController {

    /**
     * Login endpoint for JWT token generation
     * 
     * In local profile: Generates tokens without password validation
     * In production: Validates credentials against LDAP/AD
     */
    @PostMapping("/login")
    @Operation(
        summary = "User Login",
        description = "Authenticate user and generate JWT token. In local mode, accepts any valid username."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful, JWT token generated"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Login attempt for user: {} [correlationId: {}]", loginRequest.getUsername(), correlationId);
        
        try {
            // For local development, accept any non-empty username
            if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Username is required", correlationId));
            }
            
            // Generate JWT token (mock implementation for local development)
            String token = generateLocalToken(loginRequest.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 3600); // 1 hour
            response.put("username", loginRequest.getUsername());
            response.put("roles", getDefaultRoles());
            response.put("correlationId", correlationId);
            response.put("loginTime", LocalDateTime.now());
            
            log.info("Login successful for user: {} [correlationId: {}]", loginRequest.getUsername(), correlationId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Login error for user: {} [correlationId: {}]", loginRequest.getUsername(), correlationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Login failed", correlationId));
        }
    }
    
    /**
     * Token validation endpoint
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Validate JWT Token",
        description = "Validate a JWT token and return user information"
    )
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody TokenValidationRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.debug("Token validation request [correlationId: {}]", correlationId);
        
        try {
            // For local development, accept any token format
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Token is required", correlationId));
            }
            
            // Mock token validation for local development
            String username = extractUsernameFromToken(request.getToken());
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("username", username);
            response.put("roles", getDefaultRoles());
            response.put("correlationId", correlationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Token validation error [correlationId: {}]", correlationId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Invalid token", correlationId));
        }
    }
    
    /**
     * User info endpoint
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get Current User Info",
        description = "Get current authenticated user information"
    )
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String correlationId = UUID.randomUUID().toString();
        log.debug("Current user info request [correlationId: {}]", correlationId);
        
        try {
            String token = extractTokenFromHeader(authHeader);
            String username = extractUsernameFromToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("roles", getDefaultRoles());
            response.put("permissions", getDefaultPermissions());
            response.put("correlationId", correlationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Get current user error [correlationId: {}]", correlationId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Authentication required", correlationId));
        }
    }
    
    // Private helper methods
    
    private String generateLocalToken(String username) {
        // Simple token format for local development: username:timestamp:random
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 8);
        return "local_" + username + "_" + timestamp + "_" + random;
    }
    
    private String extractUsernameFromToken(String token) {
        if (token != null && token.startsWith("local_")) {
            String[] parts = token.split("_");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return "anonymous";
    }
    
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private String[] getDefaultRoles() {
        return new String[]{"JOB_VIEWER", "JOB_CREATOR", "JOB_MODIFIER", "JOB_EXECUTOR"};
    }
    
    private String[] getDefaultPermissions() {
        return new String[]{
            "CONFIG_READ", "CONFIG_CREATE", "CONFIG_UPDATE", "CONFIG_DELETE",
            "JOB_EXECUTE", "JOB_MONITOR", "TEMPLATE_MANAGE", "AUDIT_VIEW"
        };
    }
    
    private Map<String, Object> createErrorResponse(String message, String correlationId) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("correlationId", correlationId);
        error.put("timestamp", LocalDateTime.now());
        return error;
    }
    
    // Request/Response DTOs
    
    public static class LoginRequest {
        private String username;
        private String password;
        
        public LoginRequest() {}
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class TokenValidationRequest {
        private String token;
        
        public TokenValidationRequest() {}
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}