package com.fabric.batch.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * =========================================================================
 * VALID SOURCE SYSTEM ANNOTATION - DATABASE-DRIVEN VALIDATION
 * =========================================================================
 * 
 * Purpose: Custom validation annotation that verifies source system exists
 * in the CM3INT.SOURCE_SYSTEMS database table
 * 
 * Features:
 * - Database-driven validation (not hardcoded)
 * - Checks against actual SOURCE_SYSTEMS table
 * - Enterprise-grade validation for banking compliance
 * - Supports enabled/disabled source systems
 * 
 * Usage:
 * - Apply to sourceSystem fields in DTOs
 * - Automatically validates against database
 * - Provides clear error messages for invalid systems
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Master Query CRUD Implementation
 * =========================================================================
 */
@Documented
@Constraint(validatedBy = ValidSourceSystemValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSourceSystem {
    
    /**
     * Default validation message.
     */
    String message() default "Source system '{validatedValue}' does not exist in the database or is not enabled";
    
    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for additional metadata.
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to check only enabled source systems.
     * Default: true (only enabled systems are valid)
     */
    boolean enabledOnly() default true;
    
    /**
     * Whether to allow null/empty values.
     * Default: false (null/empty values are invalid)
     */
    boolean allowEmpty() default false;
}