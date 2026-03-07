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
@Schema(description = "System-wide performance metrics snapshot")
public class PerformanceMetricsDto {

    @Schema(description = "Total records processed in last 24h", example = "150000")
    private double totalThroughput;

    @Schema(description = "Average job execution time in seconds", example = "45.2")
    private double averageExecutionTime;

    @Schema(description = "Percentage of successful jobs (0-100)", example = "98.5")
    private double successRate;

    @Schema(description = "Percentage of failed jobs (0-100)", example = "1.5")
    private double errorRate;

    @Schema(description = "JVM heap memory usage percentage", example = "42.3")
    private double memoryUsage;

    @Schema(description = "System CPU load average", example = "6.5")
    private double cpuUsage;

    @Schema(description = "Active thread count", example = "25")
    private int activeConnections;

    @Schema(description = "Pending jobs in queue", example = "0")
    private int queueDepth;

    @Schema(description = "Overall system health score (0-100)", example = "95")
    private double systemHealthScore;

    @Schema(description = "Metrics collection timestamp (ISO-8601)")
    private String timestamp;
}
