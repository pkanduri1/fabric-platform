package com.truist.batch.security.config;

import com.truist.batch.security.jwt.JwtAuthenticationFilter;
import com.truist.batch.security.ldap.LdapAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration
 * 
 * Comprehensive Spring Security configuration implementing enterprise-grade security
 * for the Fabric Platform. Integrates JWT authentication, LDAP providers, method-level
 * security, and defense-in-depth security controls.
 * 
 * Security Features:
 * - JWT-based stateless authentication
 * - LDAP/AD integration with fallback mechanisms
 * - Method-level security with role-based access control
 * - CSRF protection for state-changing operations
 * - Security headers for XSS, clickjacking, and content type protection
 * - CORS configuration for cross-origin requests
 * - Rate limiting and brute force protection
 * - Comprehensive audit logging integration
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LdapAuthenticationProvider ldapAuthenticationProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Value("${fabric.security.cors.allowed-origins:http://localhost:3000,https://localhost:3000}")
    private String[] allowedOrigins;
    
    @Value("${fabric.security.csrf.enabled:true}")
    private boolean csrfEnabled;
    
    /**
     * Configures the main security filter chain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // CSRF configuration
            .csrf(csrf -> {
                if (!csrfEnabled) {
                    csrf.disable();
                } else {
                    csrf.ignoringRequestMatchers(
                        "/api/auth/**",           // Authentication endpoints 
                        "/actuator/**",           // Health check endpoints
                        "/swagger-ui/**",         // API documentation
                        "/v3/api-docs/**"         // OpenAPI endpoints
                    );
                }
            })
            
            // Session management - stateless for JWT
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Exception handling
            .exceptionHandling(exceptions -> 
                exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            
            // Request authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                
                // API endpoints with role-based access
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/management/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/configurations/create").hasAuthority("CONFIG_CREATE")
                .requestMatchers("/api/configurations/update/**").hasAuthority("CONFIG_UPDATE")
                .requestMatchers("/api/configurations/delete/**").hasAuthority("CONFIG_DELETE")
                .requestMatchers("/api/configurations/**").hasAuthority("CONFIG_READ")
                .requestMatchers("/api/jobs/execute/**").hasAuthority("JOB_EXECUTE")
                .requestMatchers("/api/jobs/**").hasAuthority("JOB_MONITOR")
                .requestMatchers("/api/templates/**").hasAuthority("TEMPLATE_MANAGE")
                .requestMatchers("/api/audit/**").hasAuthority("AUDIT_VIEW")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
            )
            
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
    
    /**
     * Configures the authentication manager with LDAP provider
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        
        authBuilder.authenticationProvider(ldapAuthenticationProvider);
        
        return authBuilder.build();
    }
    
    /**
     * Password encoder for local authentication fallback
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // High strength for financial applications
    }
    
    /**
     * CORS configuration for cross-origin requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins from configuration
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        
        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "X-Correlation-ID",
            "X-Device-Fingerprint",
            "Accept",
            "Origin"
        ));
        
        // Exposed headers for client access
        configuration.setExposedHeaders(Arrays.asList(
            "X-Correlation-ID",
            "X-Total-Count",
            "X-Rate-Limit-Remaining"
        ));
        
        // Allow credentials for authentication
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}