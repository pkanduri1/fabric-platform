package com.truist.batch.controller;

import com.truist.batch.dto.QueryPreviewRequest;
import com.truist.batch.dto.QueryPreviewResponse;
import com.truist.batch.service.TemplatePreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Template Studio SQL query preview functionality.
 * Allows users to execute read-only queries and preview results before creating templates.
 */
@RestController
@RequestMapping("/api/v2/template")
@Tag(name = "Template Preview", description = "SQL query preview for template studio")
@Slf4j
public class TemplatePreviewController {

    private final TemplatePreviewService templatePreviewService;

    public TemplatePreviewController(TemplatePreviewService templatePreviewService) {
        this.templatePreviewService = templatePreviewService;
    }

    /**
     * Preview SQL query results with read-only execution.
     *
     * @param request Query preview request containing SQL and max rows
     * @return Query results or error information
     */
    @PostMapping("/preview-query")
    @PreAuthorize("hasAnyRole('JOB_CREATOR', 'JOB_MODIFIER', 'ADMIN')")
    @Operation(
        summary = "Preview SQL query results",
        description = "Execute a read-only SQL query and return preview results (max 100 rows, 10 second timeout)"
    )
    public ResponseEntity<QueryPreviewResponse> previewQuery(@Valid @RequestBody QueryPreviewRequest request) {
        log.info("Executing query preview for SQL: {}",
                 request.getSql().length() > 50 ? request.getSql().substring(0, 50) + "..." : request.getSql());

        QueryPreviewResponse response = templatePreviewService.executePreviewQuery(
            request.getSql(),
            request.getMaxRows()
        );

        log.info("Query preview completed: success={}, rowCount={}, executionTime={}ms",
                 response.isSuccess(), response.getRowCount(), response.getExecutionTimeMs());

        return ResponseEntity.ok(response);
    }
}
