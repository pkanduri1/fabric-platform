package com.fabric.batch.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Response DTO for SQL query preview execution.
 * Contains query results and execution metadata.
 */
@Data
@Builder
public class QueryPreviewResponse {

    /**
     * Indicates if the query executed successfully.
     */
    private boolean success;

    /**
     * Column names from the query result.
     */
    private List<String> columns;

    /**
     * Row data from the query result (each row is a list of string values).
     */
    private List<List<String>> rows;

    /**
     * Total number of rows returned.
     */
    private int rowCount;

    /**
     * Query execution time in milliseconds.
     */
    private long executionTimeMs;

    /**
     * Success or error message.
     */
    private String message;
}
