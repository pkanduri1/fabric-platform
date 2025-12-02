package com.fabric.batch.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Authentication Entry Point
 * 
 * Custom authentication entry point that handles unauthorized access attempts.
 * Returns structured JSON error responses for API clients and logs security
 * events for audit and SIEM integration.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        String correlationId = UUID.randomUUID().toString();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Log the unauthorized access attempt
        log.warn("Unauthorized access attempt - Method: {}, URI: {}, IP: {}, User-Agent: {}, Correlation ID: {}, Error: {}", 
            method, requestUri, ipAddress, userAgent, correlationId, authException.getMessage());
        
        // Prepare error response
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("X-Correlation-ID", correlationId);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", 401);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", "Authentication required to access this resource");
        errorResponse.put("path", requestUri);
        errorResponse.put("correlationId", correlationId);
        
        // Don't expose internal error details in production
        if (log.isDebugEnabled()) {
            errorResponse.put("details", authException.getMessage());
        }
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}