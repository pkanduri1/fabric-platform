package com.fabric.batch.dto.monitoring;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Active or recently completed job execution")
public class ActiveJobDto {

    @Schema(description = "Unique execution identifier", example = "exec_hr_20260307_120000_abc")
    private String executionId;

    @Schema(description = "Job name", example = "atoctran")
    private String jobName;

    @Schema(description = "Source system code", example = "HR")
    private String sourceSystem;

    @Schema(description = "Execution status", example = "RUNNING")
    private String status;

    @Schema(description = "Job priority", example = "NORMAL")
    private String priority;

    @Schema(description = "Execution start time (ISO-8601)", example = "2026-03-07T12:00:00Z")
    private String startTime;

    @Schema(description = "Estimated completion time (ISO-8601)")
    private String estimatedEndTime;

    @Schema(description = "Completion percentage 0-100", example = "45")
    private int progress;

    @Schema(description = "Number of records processed", example = "15000")
    private long recordsProcessed;

    @Schema(description = "Total records expected")
    private Long totalRecords;

    @Schema(description = "Current processing stage", example = "Processing")
    private String currentStage;

    @Schema(description = "Records processed per second", example = "250.5")
    private double throughputPerSecond;

    @Schema(description = "Number of errors encountered", example = "0")
    private int errorCount;

    @Schema(description = "Number of warnings encountered", example = "0")
    private int warningCount;

    @Schema(description = "Performance score 0-100", example = "95")
    private int performanceScore;

    @Schema(description = "Trend direction", example = "STABLE")
    private String trendIndicator;

    @Schema(description = "Last heartbeat timestamp (ISO-8601)")
    private String lastHeartbeat;

    @Schema(description = "Server node running this job")
    private String assignedNode;

    @Schema(description = "Distributed tracing correlation ID")
    private String correlationId;
}
