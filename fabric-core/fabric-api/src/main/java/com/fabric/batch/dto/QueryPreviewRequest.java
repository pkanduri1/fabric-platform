package com.fabric.batch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for SQL query preview execution.
 * Used in Template Studio to preview query results before template creation.
 */
@Data
public class QueryPreviewRequest {

    /**
     * SQL query to execute (read-only, SELECT statements only).
     */
    @NotBlank(message = "SQL query is required")
    private String sql;

    /**
     * Maximum number of rows to return (1-100).
     */
    @Min(value = 1, message = "Max rows must be at least 1")
    @Max(value = 100, message = "Max rows cannot exceed 100")
    private Integer maxRows = 10;
}
