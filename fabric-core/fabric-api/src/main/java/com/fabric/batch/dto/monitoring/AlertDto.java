package com.fabric.batch.dto.monitoring;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Monitoring alert generated from job failures or performance issues")
public class AlertDto {

    @Schema(description = "Unique alert identifier", example = "alert_exec_hr_001")
    private String alertId;

    @Schema(description = "Alert type: ERROR_RATE, PERFORMANCE, THRESHOLD_BREACH", example = "ERROR_RATE")
    private String type;

    @Schema(description = "Alert severity: INFO, WARNING, ERROR, CRITICAL", example = "ERROR")
    private String severity;

    @Schema(description = "Alert title", example = "Job Failed: atoctran")
    private String title;

    @Schema(description = "Detailed alert description")
    private String description;

    @Schema(description = "Related job execution ID")
    private String jobExecutionId;

    @Schema(description = "Source system that generated the alert", example = "HR")
    private String sourceSystem;

    @Schema(description = "Alert threshold value")
    private Double threshold;

    @Schema(description = "Current metric value that triggered the alert")
    private Double currentValue;

    @Schema(description = "Alert creation timestamp (ISO-8601)")
    private String timestamp;

    @Schema(description = "Whether the alert has been acknowledged")
    private boolean acknowledged;

    @Schema(description = "User who acknowledged the alert")
    private String acknowledgedBy;

    @Schema(description = "Acknowledgement timestamp (ISO-8601)")
    private String acknowledgedAt;

    @Schema(description = "Whether the alert has been resolved")
    private boolean resolved;

    @Schema(description = "Resolution timestamp (ISO-8601)")
    private String resolvedAt;

    @Schema(description = "Whether the alert has been escalated")
    private boolean escalated;

    @Schema(description = "Current escalation level")
    private Integer escalationLevel;

    @Schema(description = "Correlation ID for tracing")
    private String correlationId;

    @Schema(description = "List of affected resource names")
    private List<String> affectedResources;
}
