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
@Schema(description = "Historical execution metrics and trends")
public class HistoricalMetricsDto {

    @Schema(description = "Aggregation period: HOUR, DAY, WEEK, MONTH", example = "DAY")
    private String period;

    @Schema(description = "Period start (ISO-8601)")
    private String startDate;

    @Schema(description = "Period end (ISO-8601)")
    private String endDate;

    @Schema(description = "Job execution count per time bucket")
    private List<TrendPoint> jobExecutionTrends;

    @Schema(description = "Records processed per time bucket")
    private List<TrendPoint> throughputTrends;

    @Schema(description = "Error rate percentage per time bucket")
    private List<TrendPoint> errorRateTrends;

    @Schema(description = "Performance score per time bucket")
    private List<TrendPoint> performanceScoreTrends;

    @Schema(description = "System health score per time bucket")
    private List<TrendPoint> systemHealthTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Single data point in a trend series")
    public static class TrendPoint {
        @Schema(description = "Time bucket timestamp (ISO-8601)")
        private String timestamp;
        @Schema(description = "Metric value", example = "42.5")
        private double value;
        @Schema(description = "Display label", example = "Jobs")
        private String label;
    }
}
