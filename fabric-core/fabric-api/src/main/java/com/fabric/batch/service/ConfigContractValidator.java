package com.fabric.batch.service;

import com.fabric.batch.model.FieldMappingConfig;
import com.fabric.batch.model.ValidationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Validates configuration contract version compatibility for save and runtime execution.
 */
@Slf4j
@Service
public class ConfigContractValidator {

    public static final String CURRENT_CONTRACT_VERSION = "1.1";
    public static final String MIN_COMPATIBLE_CONTRACT_VERSION = "1.0";

    private final ObjectMapper objectMapper;

    @Value("${batch.configContract.allowLegacyWithoutVersion:false}")
    private boolean allowLegacyWithoutVersion;

    public ConfigContractValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validateForSave(FieldMappingConfig config, ValidationResult result) {
        String version = normalize(config.getContractVersion());
        if (version == null) {
            result.addError("Missing required config contract version");
            return;
        }

        if (!isSupported(version)) {
            result.addError("Unsupported config contract version: " + version +
                    " (supported range: " + MIN_COMPATIBLE_CONTRACT_VERSION + " to " + CURRENT_CONTRACT_VERSION + ")");
        }
    }

    public void validateForExecution(String jobParametersJson) {
        try {
            if (jobParametersJson == null || jobParametersJson.isBlank()) {
                throw new IllegalStateException("Missing job parameters; contract version cannot be validated");
            }

            Map<String, Object> params = objectMapper.readValue(jobParametersJson, new TypeReference<Map<String, Object>>() {});
            Object rawVersion = readContractVersion(params);
            String version = normalize(rawVersion != null ? String.valueOf(rawVersion) : null);

            if (version == null) {
                if (allowLegacyWithoutVersion) {
                    log.warn("⚠️ Legacy configuration without contract version allowed by flag");
                    return;
                }
                throw new IllegalStateException("Missing required config contract version in job parameters");
            }

            if (!isSupported(version)) {
                throw new IllegalStateException("Unsupported config contract version for execution: " + version +
                        " (supported range: " + MIN_COMPATIBLE_CONTRACT_VERSION + " to " + CURRENT_CONTRACT_VERSION + ")");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to validate config contract version: " + e.getMessage(), e);
        }
    }

    private Object readContractVersion(Map<String, Object> params) {
        if (params.containsKey("configContractVersion")) {
            return params.get("configContractVersion");
        }
        return params.get("contractVersion");
    }

    private boolean isSupported(String version) {
        int v = parseMinor(version);
        int min = parseMinor(MIN_COMPATIBLE_CONTRACT_VERSION);
        int max = parseMinor(CURRENT_CONTRACT_VERSION);
        return v >= min && v <= max;
    }

    private int parseMinor(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return major * 100 + minor;
    }

    private String normalize(String version) {
        if (version == null) return null;
        String v = version.trim();
        return v.isEmpty() ? null : v;
    }
}
