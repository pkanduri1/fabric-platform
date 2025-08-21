package com.truist.batch.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.truist.batch.security.jwt.JwtTokenService;
import com.truist.batch.security.jwt.JwtAuthenticationFilter;
import com.truist.batch.security.jwt.JwtTokenException;
import com.truist.batch.security.jwt.UserTokenDetails;
import com.truist.batch.security.service.TokenBlacklistService;
import com.truist.batch.testutils.BaseTestNGTest;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;

/**
 * Security tests for JWT authentication and authorization
 * Tests token generation, validation, expiration, and security enforcement
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */
@Slf4j
@Test(groups = {"security", "authentication", "jwt"})
public class JwtAuthenticationTest extends BaseTestNGTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;
    
    @InjectMocks
    private JwtTokenService jwtTokenService;
    
    private UserDetails testUser;
    private Collection<GrantedAuthority> userAuthorities;
    private String testUsername;
    private String testPassword;
    private SecretKey testSecretKey;
    private long tokenExpiration;
    private String correlationId;
    
    // Test constants
    private static final String SECRET_KEY = "test-secret-key-for-fabric-platform-jwt-testing-purposes-only-256-bits-long";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 7200000; // 2 hours
    
    @BeforeMethod
    @Override
    public void setUp() {
        super.setUp();
        initializeTestData();
        setupMockBehavior();
    }
    
    private void initializeTestData() {
        testUsername = "test-user-" + System.currentTimeMillis();
        testPassword = "test-password";
        correlationId = generateCorrelationId();
        tokenExpiration = ACCESS_TOKEN_EXPIRATION;
        
        // Initialize test secret key
        testSecretKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        
        // Initialize test user authorities
        userAuthorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_JOB_CREATOR"),
            new SimpleGrantedAuthority("ROLE_JOB_MODIFIER"),
            new SimpleGrantedAuthority("ROLE_USER")
        );
        
        // Create test user
        testUser = User.builder()
            .username(testUsername)
            .password(testPassword)
            .authorities(userAuthorities)
            .build();
    }
    
    private void setupMockBehavior() {
        // Default: tokens are not blacklisted
        when(tokenBlacklistService.isTokenBlacklisted(anyString()))
            .thenReturn(false);
            
        doNothing().when(tokenBlacklistService)
            .blacklistToken(anyString(), any(Date.class));
    }
    
    @Test(description = "Should generate valid JWT access token")
    public void testGenerateAccessToken_Success() {
        log.info("Testing JWT access token generation for user: {}", testUsername);
        
        // When
        String accessToken = jwtTokenService.generateAccessToken(testUser, correlationId);
        
        // Then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        assertThat(accessToken.split("\\.")).hasSize(3); // JWT has 3 parts
        
        // Verify token structure
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(testSecretKey)
            .build()
            .parseClaimsJws(accessToken)
            .getBody();
            
        assertThat(claims.getSubject()).isEqualTo(testUsername);
        assertThat(claims.get("type")).isEqualTo("access");
        assertThat(claims.get("correlationId")).isEqualTo(correlationId);
        assertThat(claims.get("roles")).isNotNull();
        
        log.info("Access token generated successfully for user: {}", testUsername);
    }
    
    @Test(description = "Should generate valid JWT refresh token")
    public void testGenerateRefreshToken_Success() {
        log.info("Testing JWT refresh token generation for user: {}", testUsername);
        
        // When
        String refreshToken = jwtTokenService.generateRefreshToken(testUsername, correlationId);
        
        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        
        // Verify token structure
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(testSecretKey)
            .build()
            .parseClaimsJws(refreshToken)
            .getBody();
            
        assertThat(claims.getSubject()).isEqualTo(testUsername);
        assertThat(claims.get("type")).isEqualTo("refresh");
        assertThat(claims.get("correlationId")).isEqualTo(correlationId);
        
        log.info("Refresh token generated successfully for user: {}", testUsername);
    }
    
    @Test(description = "Should validate JWT token successfully")
    public void testValidateToken_Success() {
        log.info("Testing JWT token validation");
        
        // Given
        String accessToken = jwtTokenService.generateAccessToken(testUser, correlationId);
        
        // When
        boolean isValid = jwtTokenService.isValidToken(accessToken);
        
        // Then
        assertThat(isValid).isTrue();
        
        log.info("JWT token validation successful");
    }
    
    @Test(description = "Should reject invalid JWT token")
    public void testValidateToken_Invalid() {
        log.info("Testing JWT token validation with invalid token");
        
        // Given
        String invalidToken = "invalid.jwt.token";
        
        // When
        boolean isValid = jwtTokenService.isValidToken(invalidToken);
        
        // Then
        assertThat(isValid).isFalse();
        
        log.info("Invalid JWT token correctly rejected");
    }
    
    @Test(description = "Should reject expired JWT token")
    public void testValidateToken_Expired() {
        log.info("Testing JWT token validation with expired token");
        
        // Given - Create expired token
        Date expiredDate = new Date(System.currentTimeMillis() - 3600000); // 1 hour ago
        String expiredToken = Jwts.builder()
            .setSubject(testUsername)
            .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
            .setExpiration(expiredDate)
            .claim("type", "access")
            .claim("correlationId", correlationId)
            .signWith(testSecretKey, SignatureAlgorithm.HS256)
            .compact();
        
        // When
        boolean isValid = jwtTokenService.isValidToken(expiredToken);
        
        // Then
        assertThat(isValid).isFalse();
        
        log.info("Expired JWT token correctly rejected");
    }
    
    @Test(description = "Should reject blacklisted JWT token")
    public void testValidateToken_Blacklisted() {
        log.info("Testing JWT token validation with blacklisted token");
        
        // Given
        String accessToken = jwtTokenService.generateAccessToken(testUser, correlationId);
        
        // Mock token as blacklisted
        when(tokenBlacklistService.isTokenBlacklisted(accessToken))
            .thenReturn(true);
        
        // When
        boolean isValid = jwtTokenService.isValidToken(accessToken);
        
        // Then
        assertThat(isValid).isFalse();
        
        verify(tokenBlacklistService).isTokenBlacklisted(accessToken);
        log.info("Blacklisted JWT token correctly rejected");
    }
    
    @Test(description = "Should extract username from JWT token")
    public void testExtractUsername_Success() {
        log.info("Testing username extraction from JWT token");
        
        // Given
        String accessToken = jwtTokenService.generateAccessToken(testUser, correlationId);
        
        // When
        String extractedUsername = jwtTokenService.extractUsername(accessToken);
        
        // Then
        assertThat(extractedUsername).isEqualTo(testUsername);
        
        log.info("Username extracted successfully: {}", extractedUsername);
    }
    
    @Test(description = "Should extract roles from JWT token")
    public void testExtractRoles_Success() {
        log.info("Testing roles extraction from JWT token");
        
        // Given
        String accessToken = jwtTokenService.generateAccessToken(testUser, correlationId);
        
        // When
        Collection<GrantedAuthority> extractedRoles = jwtTokenService.extractRoles(accessToken);
        
        // Then
        assertThat(extractedRoles).isNotEmpty();
        assertThat(extractedRoles).hasSize(userAuthorities.size());
        assertThat(extractedRoles).containsAll(userAuthorities);
        
        log.info("Roles extracted successfully: {}", extractedRoles);
    }
    
    @Test(description = "Should create UserTokenDetails from JWT token")
    public void testCreateUserTokenDetails_Success() {
        log.info("Testing UserTokenDetails creation from JWT token");
        
        // Given
        String accessToken = jwtTokenService.generateAccessToken(testUser, correlationId);
        
        // When
        UserTokenDetails userTokenDetails = jwtTokenService.createUserTokenDetails(accessToken);
        
        // Then
        assertThat(userTokenDetails).isNotNull();
        assertThat(userTokenDetails.getUsername()).isEqualTo(testUsername);
        assertThat(userTokenDetails.getAuthorities()).containsAll(userAuthorities);
        assertThat(userTokenDetails.isAccountNonExpired()).isTrue();
        assertThat(userTokenDetails.isAccountNonLocked()).isTrue();
        assertThat(userTokenDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userTokenDetails.isEnabled()).isTrue();
        
        log.info("UserTokenDetails created successfully: {}", userTokenDetails.getUsername());
    }
    
    @Test(description = "Should handle JWT token refresh successfully")
    public void testRefreshToken_Success() {
        log.info("Testing JWT token refresh");
        
        // Given
        String refreshToken = jwtTokenService.generateRefreshToken(testUsername, correlationId);
        
        // When
        String newAccessToken = jwtTokenService.refreshAccessToken(refreshToken, testUser);
        
        // Then
        assertThat(newAccessToken).isNotNull();
        assertThat(newAccessToken).isNotEmpty();
        assertThat(jwtTokenService.isValidToken(newAccessToken)).isTrue();
        assertThat(jwtTokenService.extractUsername(newAccessToken)).isEqualTo(testUsername);
        
        log.info("JWT token refresh successful");
    }
    
    @Test(description = "Should fail token refresh with invalid refresh token")
    public void testRefreshToken_InvalidRefreshToken() {
        log.info("Testing JWT token refresh with invalid refresh token");
        
        // Given
        String invalidRefreshToken = "invalid.refresh.token";
        
        // When & Then
        assertThatThrownBy(() -> 
            jwtTokenService.refreshAccessToken(invalidRefreshToken, testUser))
            .isInstanceOf(JwtTokenException.class)
            .hasMessageContaining("Invalid refresh token");
        
        log.info("Invalid refresh token correctly rejected");
    }
    
    @Test(description = "Should blacklist JWT token successfully")
    public void testBlacklistToken_Success() {
        log.info("Testing JWT token blacklisting");
        
        // Given
        String accessToken = jwtTokenService.generateAccessToken(testUser, correlationId);
        
        // When
        jwtTokenService.blacklistToken(accessToken);
        
        // Then
        verify(tokenBlacklistService).blacklistToken(eq(accessToken), any(Date.class));
        log.info("JWT token blacklisted successfully");
    }
    
    @Test(description = "Should enforce role-based access control")
    public void testRoleBasedAccessControl() {
        log.info("Testing role-based access control enforcement");
        
        // Given - User with limited roles
        Collection<GrantedAuthority> limitedAuthorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_JOB_VIEWER")
        );
        
        UserDetails limitedUser = User.builder()
            .username("limited-user")
            .password("password")
            .authorities(limitedAuthorities)
            .build();
        
        String accessToken = jwtTokenService.generateAccessToken(limitedUser, correlationId);
        
        // When
        Collection<GrantedAuthority> extractedRoles = jwtTokenService.extractRoles(accessToken);
        
        // Then
        assertThat(extractedRoles).hasSize(1);
        assertThat(extractedRoles).contains(new SimpleGrantedAuthority("ROLE_JOB_VIEWER"));
        assertThat(extractedRoles).doesNotContain(new SimpleGrantedAuthority("ROLE_JOB_CREATOR"));
        
        log.info("Role-based access control enforced correctly");
    }
    
    @Test(description = "Should handle concurrent token operations safely")
    public void testConcurrentTokenOperations() {
        log.info("Testing concurrent JWT token operations");
        
        // Given
        int threadCount = 10;
        String[] tokens = new String[threadCount];
        
        // When - Generate tokens concurrently
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                tokens[index] = jwtTokenService.generateAccessToken(testUser, correlationId + "-" + index);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        
        // Then - All tokens should be valid and unique
        for (int i = 0; i < threadCount; i++) {
            assertThat(tokens[i]).isNotNull();
            assertThat(jwtTokenService.isValidToken(tokens[i])).isTrue();
            
            // Verify uniqueness
            for (int j = i + 1; j < threadCount; j++) {
                assertThat(tokens[i]).isNotEqualTo(tokens[j]);
            }
        }
        
        log.info("Concurrent JWT token operations completed successfully");
    }
    
    @Test(description = "Should validate token signature integrity")
    public void testTokenSignatureIntegrity() {
        log.info("Testing JWT token signature integrity");
        
        // Given
        String accessToken = jwtTokenService.generateAccessToken(testUser, correlationId);
        
        // When - Try to tamper with token
        String[] tokenParts = accessToken.split("\\.");
        String tamperedToken = tokenParts[0] + ".tampered." + tokenParts[2];
        
        // Then
        boolean isValid = jwtTokenService.isValidToken(tamperedToken);
        assertThat(isValid).isFalse();
        
        log.info("Token signature integrity validation successful");
    }
    
    @Test(description = "Should handle token expiration grace period")
    public void testTokenExpirationGracePeriod() {
        log.info("Testing JWT token expiration grace period");
        
        // Given - Token that expires very soon
        Date nearExpirationDate = new Date(System.currentTimeMillis() + 1000); // 1 second
        String nearExpiredToken = Jwts.builder()
            .setSubject(testUsername)
            .setIssuedAt(new Date())
            .setExpiration(nearExpirationDate)
            .claim("type", "access")
            .claim("correlationId", correlationId)
            .signWith(testSecretKey, SignatureAlgorithm.HS256)
            .compact();
        
        // When - Token is still valid
        boolean isValid = jwtTokenService.isValidToken(nearExpiredToken);
        assertThat(isValid).isTrue();
        
        // Wait for expiration
        waitForAsync(2000);
        
        // Then - Token should now be expired
        isValid = jwtTokenService.isValidToken(nearExpiredToken);
        assertThat(isValid).isFalse();
        
        log.info("Token expiration grace period test completed");
    }
    
    @Override
    protected void cleanupTestData() {
        // Reset mocks
        reset(tokenBlacklistService);
        log.debug("Test data cleanup completed for JwtAuthenticationTest");
    }
}