package com.truist.batch.validation;

import com.truist.batch.entity.SourceSystemEntity;
import com.truist.batch.repository.SourceSystemRepository;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * =========================================================================
 * VALID SOURCE SYSTEM VALIDATOR TESTS - DATABASE-DRIVEN VALIDATION
 * =========================================================================
 * 
 * Purpose: Unit tests for ValidSourceSystemValidator to ensure proper
 * database-driven validation of source systems
 * 
 * Test Scenarios:
 * - Valid enabled source systems (MTG, SHAW, ENCORE)
 * - Invalid/non-existent source systems
 * - Disabled source systems (when enabledOnly=true)
 * - Null/empty values handling
 * - Exception handling and error messages
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Master Query CRUD Implementation
 * =========================================================================
 */
@ExtendWith(MockitoExtension.class)
class ValidSourceSystemValidatorTest {

    @Mock
    private SourceSystemRepository sourceSystemRepository;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @InjectMocks
    private ValidSourceSystemValidator validator;

    @BeforeEach
    void setUp() {
        // Setup default mock behavior only when needed
        lenient().when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        lenient().when(violationBuilder.addConstraintViolation())
                .thenReturn(context);
    }

    @Test
    void testValidSourceSystem_MTG_ShouldPass() {
        // Given
        initializeValidatorWithDefaults();
        setupSourceSystemExists("MTG", true, true);

        // When
        boolean result = validator.isValid("MTG", context);

        // Then
        assertTrue(result);
        verify(sourceSystemRepository).existsByIdIgnoreCase("MTG");
        verify(sourceSystemRepository).findById("MTG");
    }

    @Test
    void testValidSourceSystem_SHAW_ShouldPass() {
        // Given
        initializeValidatorWithDefaults();
        setupSourceSystemExists("SHAW", true, true);

        // When
        boolean result = validator.isValid("SHAW", context);

        // Then
        assertTrue(result);
        verify(sourceSystemRepository).existsByIdIgnoreCase("SHAW");
        verify(sourceSystemRepository).findById("SHAW");
    }

    @Test
    void testValidSourceSystem_ENCORE_ShouldPass() {
        // Given
        initializeValidatorWithDefaults();
        setupSourceSystemExists("ENCORE", true, true);

        // When
        boolean result = validator.isValid("ENCORE", context);

        // Then
        assertTrue(result);
        verify(sourceSystemRepository).existsByIdIgnoreCase("ENCORE");
        verify(sourceSystemRepository).findById("ENCORE");
    }

    @Test
    void testInvalidSourceSystem_ShouldFail() {
        // Given
        initializeValidatorWithDefaults();
        when(sourceSystemRepository.existsByIdIgnoreCase("INVALID")).thenReturn(false);

        // When
        boolean result = validator.isValid("INVALID", context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Source system 'INVALID' does not exist in the database");
    }

    @Test
    void testDisabledSourceSystem_WhenEnabledOnly_ShouldFail() {
        // Given
        initializeValidatorWithDefaults();
        setupSourceSystemExists("DISABLED_SYSTEM", true, false); // exists but disabled

        // When
        boolean result = validator.isValid("DISABLED_SYSTEM", context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Source system 'DISABLED_SYSTEM' exists but is currently disabled");
    }

    @Test
    void testDisabledSourceSystem_WhenEnabledOnlyFalse_ShouldPass() {
        // Given
        initializeValidator(false, false); // enabledOnly=false, allowEmpty=false
        setupSourceSystemExists("DISABLED_SYSTEM", true, false); // exists but disabled

        // When
        boolean result = validator.isValid("DISABLED_SYSTEM", context);

        // Then
        assertTrue(result);
        verify(sourceSystemRepository).existsByIdIgnoreCase("DISABLED_SYSTEM");
        // Should not check findById when enabledOnly=false
        verify(sourceSystemRepository, never()).findById(anyString());
    }

    @Test
    void testNullValue_WhenAllowEmptyTrue_ShouldPass() {
        // Given
        initializeValidator(true, true); // enabledOnly=true, allowEmpty=true

        // When
        boolean result = validator.isValid(null, context);

        // Then
        assertTrue(result);
        verify(sourceSystemRepository, never()).existsByIdIgnoreCase(anyString());
    }

    @Test
    void testNullValue_WhenAllowEmptyFalse_ShouldFail() {
        // Given
        initializeValidatorWithDefaults(); // allowEmpty=false by default

        // When
        boolean result = validator.isValid(null, context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Source system cannot be null or empty");
    }

    @Test
    void testEmptyString_WhenAllowEmptyFalse_ShouldFail() {
        // Given
        initializeValidatorWithDefaults();

        // When
        boolean result = validator.isValid("", context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Source system cannot be null or empty");
    }

    @Test
    void testCaseInsensitiveValidation_ShouldPass() {
        // Given
        initializeValidatorWithDefaults();
        setupSourceSystemExists("MTG", true, true);

        // When - test with lowercase
        boolean result = validator.isValid("mtg", context);

        // Then
        assertTrue(result);
        verify(sourceSystemRepository).existsByIdIgnoreCase("MTG"); // Should be normalized to uppercase
    }

    @Test
    void testRepositoryException_ShouldFail() {
        // Given
        initializeValidatorWithDefaults();
        when(sourceSystemRepository.existsByIdIgnoreCase(anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        boolean result = validator.isValid("MTG", context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                argThat(message -> message.contains("Source system validation failed due to system error")));
    }

    /**
     * Helper method to initialize validator with default settings.
     */
    private void initializeValidatorWithDefaults() {
        initializeValidator(true, false); // enabledOnly=true, allowEmpty=false
    }

    /**
     * Helper method to initialize validator with custom settings.
     */
    private void initializeValidator(boolean enabledOnly, boolean allowEmpty) {
        ValidSourceSystem annotation = mock(ValidSourceSystem.class);
        when(annotation.enabledOnly()).thenReturn(enabledOnly);
        when(annotation.allowEmpty()).thenReturn(allowEmpty);
        validator.initialize(annotation);
    }

    /**
     * Helper method to setup source system repository mocks.
     */
    private void setupSourceSystemExists(String systemId, boolean exists, boolean enabled) {
        lenient().when(sourceSystemRepository.existsByIdIgnoreCase(systemId)).thenReturn(exists);
        
        if (exists) {
            SourceSystemEntity entity = new SourceSystemEntity();
            entity.setId(systemId);
            entity.setEnabled(enabled ? "Y" : "N");
            lenient().when(sourceSystemRepository.findById(systemId)).thenReturn(Optional.of(entity));
        } else {
            lenient().when(sourceSystemRepository.findById(systemId)).thenReturn(Optional.empty());
        }
    }
}