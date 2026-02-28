package com.fabric.batch.service;

import com.fabric.batch.model.FieldMappingConfig;
import com.fabric.batch.model.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ConfigContractValidatorTest {

    private final ConfigContractValidator validator = new ConfigContractValidator(new ObjectMapper());

    @Test
    void validateForSave_rejectsMissingVersion() {
        FieldMappingConfig config = new FieldMappingConfig();
        ValidationResult result = new ValidationResult();

        validator.validateForSave(config, result);

        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Missing required config contract version")));
    }

    @Test
    void validateForSave_acceptsSupportedVersion() {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setContractVersion("1.1");
        ValidationResult result = new ValidationResult();

        validator.validateForSave(config, result);

        assertFalse(result.hasErrors());
    }

    @Test
    void validateForExecution_rejectsUnsupportedVersion() {
        String params = "{\"configContractVersion\":\"2.0\"}";

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.validateForExecution(params));

        assertTrue(ex.getMessage().contains("Unsupported config contract version"));
    }

    @Test
    void validateForExecution_allowsLegacyWhenFlagEnabled() {
        ReflectionTestUtils.setField(validator, "allowLegacyWithoutVersion", true);
        assertDoesNotThrow(() -> validator.validateForExecution("{\"batchDate\":\"2026-02-28\"}"));
    }
}
