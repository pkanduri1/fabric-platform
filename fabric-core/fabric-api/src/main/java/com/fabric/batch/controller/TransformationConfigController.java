package com.fabric.batch.controller;

import com.fabric.batch.entity.FieldTemplateEntity;
import com.fabric.batch.service.TransformationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/transformation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transformation Configuration", description = "CRUD for field transformation configs")
public class TransformationConfigController {

    private final TransformationConfigService service;

    // ============================================================================
    // Transformation Configuration CRUD
    // ============================================================================

    @GetMapping("/configs")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "List all transformation configurations")
    public ResponseEntity<List<FieldTemplateEntity>> getAllConfigurations() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/configs/{configId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Get transformation configuration by ID")
    public ResponseEntity<FieldTemplateEntity> getConfigurationById(@PathVariable String configId) {
        return ResponseEntity.ok(service.findById(configId));
    }

    @GetMapping("/configs/by-system/{sourceSystem}")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Get configurations by source system (file type)")
    public ResponseEntity<List<FieldTemplateEntity>> getConfigurationsBySourceSystem(
            @PathVariable String sourceSystem) {
        return ResponseEntity.ok(service.findBySourceSystem(sourceSystem));
    }

    @PostMapping("/configs")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create new transformation configuration")
    public ResponseEntity<FieldTemplateEntity> createConfiguration(
            @RequestBody FieldTemplateEntity entity) {
        return ResponseEntity.status(201).body(service.create(entity));
    }

    @PutMapping("/configs/{configId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update transformation configuration")
    public ResponseEntity<FieldTemplateEntity> updateConfiguration(
            @PathVariable String configId, @RequestBody FieldTemplateEntity entity) {
        return ResponseEntity.ok(service.update(configId, entity));
    }

    @DeleteMapping("/configs/{configId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Soft-delete transformation configuration")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable String configId) {
        service.softDelete(configId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================================
    // Field Transformation Rule Management (stubs — not yet implemented)
    // ============================================================================

    @GetMapping("/configs/{configId}/rules")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Get field transformation rules")
    public ResponseEntity<List<Object>> getTransformationRules(@PathVariable String configId) {
        log.info("Stub: Retrieved transformation rules for config: {}", configId);
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Get field transformation rule by ID")
    public ResponseEntity<Object> getTransformationRule(@PathVariable Long ruleId) {
        log.info("Stub: Retrieved transformation rule: {}", ruleId);
        return ResponseEntity.ok(Map.of("ruleId", ruleId, "message", "Rule stub"));
    }

    @PostMapping("/configs/{configId}/rules")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create field transformation rule")
    public ResponseEntity<Object> createTransformationRule(
            @PathVariable String configId, @RequestBody Map<String, Object> rule) {
        log.info("Stub: Created transformation rule for config: {}", configId);
        return ResponseEntity.status(201).body(Map.of("configId", configId, "message", "Rule created (stub)", "rule", rule));
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update field transformation rule")
    public ResponseEntity<Object> updateTransformationRule(
            @PathVariable Long ruleId, @RequestBody Map<String, Object> rule) {
        log.info("Stub: Updated transformation rule: {}", ruleId);
        return ResponseEntity.ok(Map.of("ruleId", ruleId, "message", "Rule updated (stub)", "rule", rule));
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Delete field transformation rule")
    public ResponseEntity<Void> deleteTransformationRule(@PathVariable Long ruleId) {
        log.info("Stub: Disabled transformation rule: {}", ruleId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================================
    // Statistics and Reporting (stubs — not yet implemented)
    // ============================================================================

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Get transformation statistics")
    public ResponseEntity<Map<String, Object>> getTransformationStatistics() {
        log.info("Stub: Retrieved transformation statistics");
        return ResponseEntity.ok(Map.of(
                "totalConfigurations", 0,
                "enabledConfigurations", 0,
                "validationEnabledConfigurations", 0,
                "averageFieldsPerConfiguration", 0));
    }

    @GetMapping("/configs/{configId}/stats")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Get configuration rule statistics")
    public ResponseEntity<Map<String, Object>> getConfigurationStatistics(@PathVariable String configId) {
        log.info("Stub: Retrieved statistics for configuration: {}", configId);
        return ResponseEntity.ok(Map.of(
                "totalRules", 0,
                "enabledRules", 0,
                "requiredFields", 0,
                "rulesWithValidation", 0));
    }
}
