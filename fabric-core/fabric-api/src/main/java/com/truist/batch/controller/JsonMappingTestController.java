package com.truist.batch.controller;

import com.truist.batch.service.JsonMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test controller to validate JsonMappingService and JsonMappingAdapter integration.
 * 
 * This controller provides endpoints to test the dependency injection and 
 * functionality of the JsonMappingService and JsonMappingAdapter components.
 */
@RestController
@RequestMapping("/api/v2/test/json-mapping")
@RequiredArgsConstructor
@Slf4j
public class JsonMappingTestController {

    private final JsonMappingService jsonMappingService;

    /**
     * Simple health check endpoint to verify dependency injection.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("🔄 Testing JsonMappingService dependency injection");
        
        try {
            // Test basic functionality
            String testJson = "{\"test\": \"value\", \"number\": 123}";
            Map<String, Object> parsedData = jsonMappingService.parseJsonToMap(testJson);
            
            Map<String, Object> response = Map.of(
                "status", "OK",
                "message", "JsonMappingService is working correctly",
                "testResult", parsedData,
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("✅ JsonMappingService health check passed");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ JsonMappingService health check failed: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = Map.of(
                "status", "ERROR",
                "message", "JsonMappingService test failed: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Test JSON to YAML conversion functionality.
     */
    @PostMapping("/json-to-yaml")
    public ResponseEntity<Map<String, Object>> testJsonToYaml(@RequestBody Map<String, Object> jsonData) {
        log.info("🔄 Testing JSON to YAML conversion");
        
        try {
            String jsonString = jsonMappingService.processDataMapping(jsonData);
            String yamlString = jsonMappingService.jsonConfigurationToYaml(jsonString);
            
            Map<String, Object> response = Map.of(
                "status", "OK",
                "originalJson", jsonData,
                "convertedYaml", yamlString,
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("✅ JSON to YAML conversion test passed");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ JSON to YAML conversion failed: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = Map.of(
                "status", "ERROR",
                "message", "JSON to YAML conversion failed: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}