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
@Schema(description = "Monitoring dashboard aggregate response")
public class MonitoringDashboardResponse {

    @Schema(description = "Currently running or started jobs")
    private List<ActiveJobDto> activeJobs;

    @Schema(description = "Jobs completed in last 24 hours")
    private List<ActiveJobDto> recentCompletions;

    @Schema(description = "System-wide performance metrics")
    private PerformanceMetricsDto performanceMetrics;

    @Schema(description = "System health status")
    private SystemHealthDto systemHealth;

    @Schema(description = "Active alerts from failed or long-running jobs")
    private List<AlertDto> alerts;

    @Schema(description = "Historical execution trends (24h)")
    private HistoricalMetricsDto trends;

    @Schema(description = "ISO-8601 timestamp of this response", example = "2026-03-07T12:00:00Z")
    private String lastUpdate;

    @Schema(description = "Correlation ID for request tracing", example = "mon_a1b2c3d4")
    private String correlationId;
}
