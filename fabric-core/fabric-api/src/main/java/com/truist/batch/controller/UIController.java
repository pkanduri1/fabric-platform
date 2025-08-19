package com.truist.batch.controller;

import com.truist.batch.dto.SourceSystemInfo;
import com.truist.batch.model.FileTypeTemplate;
import com.truist.batch.service.SourceSystemService;
import com.truist.batch.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UI Controller for Frontend Endpoint Mapping
 * 
 * This controller provides UI-specific endpoints that map to existing
 * admin endpoints to maintain frontend compatibility while preserving
 * the existing admin API structure.
 * 
 * Frontend expects /ui/* endpoints but backend has /api/admin/* endpoints.
 * This controller bridges the gap without duplicating business logic.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since UI Integration Fix
 */
@RestController
@RequestMapping("/ui")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000"})
public class UIController {

    private final SourceSystemService sourceSystemService;
    private final TemplateService templateService;

    /**
     * Frontend expects: GET /ui/source-systems
     * Maps to: /api/admin/source-systems functionality
     */
    @GetMapping("/source-systems")
    public ResponseEntity<List<SourceSystemInfo>> getSourceSystems() {
        try {
            log.info("UI: Fetching all source systems for dropdown");
            List<SourceSystemInfo> sourceSystems = sourceSystemService.getAllEnabledSourceSystems();
            log.info("UI: Found {} enabled source systems", sourceSystems.size());
            return ResponseEntity.ok(sourceSystems);
        } catch (Exception e) {
            log.error("UI: Error fetching source systems", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Frontend expects: GET /ui/templates/file-types
     * Maps to: /admin/templates/file-types functionality
     */
    @GetMapping("/templates/file-types")
    public ResponseEntity<List<FileTypeTemplate>> getFileTypes() {
        try {
            log.info("UI: Fetching all file types for dropdown");
            List<FileTypeTemplate> fileTypes = templateService.getAllFileTypes();
            log.info("UI: Found {} file types", fileTypes.size());
            return ResponseEntity.ok(fileTypes);
        } catch (Exception e) {
            log.error("UI: Error fetching file types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check for UI endpoints
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UI Controller is healthy");
    }
}
