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
@Schema(description = "System health status across all components")
public class SystemHealthDto {

    @Schema(description = "Overall health score 0-100", example = "95")
    private int overallScore;

    @Schema(description = "Database health status")
    private ComponentHealth database;

    @Schema(description = "WebSocket subsystem health")
    private ComponentHealth webSocket;

    @Schema(description = "Batch processing subsystem health")
    private ComponentHealth batchProcessing;

    @Schema(description = "JVM memory health")
    private MemoryHealth memory;

    @Schema(description = "Last health check timestamp (ISO-8601)")
    private String lastCheck;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual component health")
    public static class ComponentHealth {
        @Schema(description = "Health status: HEALTHY, DEGRADED, or DOWN", example = "HEALTHY")
        private String status;
        @Schema(description = "Response time in milliseconds", example = "4.0")
        private double responseTime;
        @Schema(description = "Connection pool size", example = "10")
        private int connectionPool;
        @Schema(description = "Active connections", example = "5")
        private int activeConnections;
        @Schema(description = "Messages per second", example = "10.0")
        private double messageRate;
        @Schema(description = "Number of active jobs", example = "3")
        private int activeJobs;
        @Schema(description = "Queue length", example = "0")
        private int queueLength;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "JVM memory health details")
    public static class MemoryHealth {
        @Schema(description = "Memory status: HEALTHY, WARNING, or CRITICAL", example = "HEALTHY")
        private String status;
        @Schema(description = "Used memory in MB", example = "512")
        private long used;
        @Schema(description = "Available memory in MB", example = "1536")
        private long available;
        @Schema(description = "Memory usage percentage", example = "25.0")
        private double percentage;
    }
}
