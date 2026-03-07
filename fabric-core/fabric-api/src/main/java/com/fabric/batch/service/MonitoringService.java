package com.fabric.batch.service;

import com.fabric.batch.dto.monitoring.*;
import com.fabric.batch.repository.MonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final MonitoringRepository monitoringRepository;

    public MonitoringDashboardResponse getDashboardData() {
        String correlationId = "mon_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Building monitoring dashboard data correlationId={}", correlationId);

        List<ActiveJobDto> activeJobs = monitoringRepository.findActiveJobs(50);
        List<ActiveJobDto> recentCompletions = monitoringRepository.findRecentCompletions(20);
        PerformanceMetricsDto performanceMetrics = buildPerformanceMetrics();
        SystemHealthDto systemHealth = buildSystemHealth(activeJobs.size());
        List<AlertDto> alerts = buildAlerts(activeJobs);
        HistoricalMetricsDto trends = buildTrends();

        return MonitoringDashboardResponse.builder()
                .activeJobs(activeJobs)
                .recentCompletions(recentCompletions)
                .performanceMetrics(performanceMetrics)
                .systemHealth(systemHealth)
                .alerts(alerts)
                .trends(trends)
                .lastUpdate(Instant.now().toString())
                .correlationId(correlationId)
                .build();
    }

    private PerformanceMetricsDto buildPerformanceMetrics() {
        double successRate = 100.0;
        double avgDuration = 0;
        double totalThroughput = 0;

        Map<String, Number> stats = monitoringRepository.getExecutionStats24h();
        long total = stats.get("total").longValue();
        long successCount = stats.get("successCount").longValue();

        if (total > 0) {
            successRate = Math.round((double) successCount / total * 10000.0) / 100.0;
        }
        avgDuration = stats.get("avgDuration").doubleValue();
        totalThroughput = stats.get("totalRecords").doubleValue();

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        double cpuLoad = os.getSystemLoadAverage();
        long usedMem = mem.getHeapMemoryUsage().getUsed();
        long maxMem = mem.getHeapMemoryUsage().getMax();
        double memPct = maxMem > 0 ? Math.round((double) usedMem / maxMem * 10000.0) / 100.0 : 0;

        return PerformanceMetricsDto.builder()
                .totalThroughput(totalThroughput)
                .averageExecutionTime(Math.round(avgDuration * 100.0) / 100.0)
                .successRate(successRate)
                .errorRate(Math.round((100 - successRate) * 100.0) / 100.0)
                .memoryUsage(memPct)
                .cpuUsage(cpuLoad >= 0 ? Math.round(cpuLoad * 100.0) / 100.0 : 5.0)
                .activeConnections(Thread.activeCount())
                .queueDepth(0)
                .systemHealthScore(successRate >= 90 ? 95 : (successRate >= 70 ? 80 : 60))
                .timestamp(Instant.now().toString())
                .build();
    }

    private SystemHealthDto buildSystemHealth(int activeJobCount) {
        double dbResponseTime = monitoringRepository.getDatabaseResponseTimeMs();
        String dbStatus = dbResponseTime < 0 ? "DOWN" : (dbResponseTime > 1000 ? "DEGRADED" : "HEALTHY");

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long usedMem = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long maxMem = mem.getHeapMemoryUsage().getMax() / (1024 * 1024);
        double memPct = maxMem > 0 ? Math.round((double) usedMem / maxMem * 10000.0) / 100.0 : 0;
        String memStatus = memPct < 70 ? "HEALTHY" : (memPct < 85 ? "WARNING" : "CRITICAL");

        int overallScore = 95;
        if ("DOWN".equals(dbStatus)) overallScore = 20;
        else if ("DEGRADED".equals(dbStatus)) overallScore = 70;
        if ("CRITICAL".equals(memStatus)) overallScore = Math.min(overallScore, 50);
        else if ("WARNING".equals(memStatus)) overallScore = Math.min(overallScore, 75);

        return SystemHealthDto.builder()
                .overallScore(overallScore)
                .database(SystemHealthDto.ComponentHealth.builder()
                        .status(dbStatus)
                        .responseTime(dbResponseTime)
                        .connectionPool(10)
                        .build())
                .webSocket(SystemHealthDto.ComponentHealth.builder()
                        .status("HEALTHY")
                        .activeConnections(0)
                        .messageRate(0)
                        .build())
                .batchProcessing(SystemHealthDto.ComponentHealth.builder()
                        .status("HEALTHY")
                        .activeJobs(activeJobCount)
                        .queueLength(0)
                        .build())
                .memory(SystemHealthDto.MemoryHealth.builder()
                        .status(memStatus)
                        .used(usedMem)
                        .available(maxMem - usedMem)
                        .percentage(memPct)
                        .build())
                .lastCheck(Instant.now().toString())
                .build();
    }

    private List<AlertDto> buildAlerts(List<ActiveJobDto> activeJobs) {
        List<AlertDto> alerts = new ArrayList<>();

        List<Map<String, Object>> failedExecs = monitoringRepository.findFailedExecutions24h(20);
        for (Map<String, Object> row : failedExecs) {
            String errorMsg = row.get("errorMessage") != null
                    ? (String) row.get("errorMessage")
                    : "Job execution failed";
            alerts.add(AlertDto.builder()
                    .alertId("alert_" + row.get("executionId"))
                    .type("ERROR_RATE")
                    .severity("ERROR")
                    .title("Job Failed: " + row.get("jobName"))
                    .description(errorMsg)
                    .jobExecutionId((String) row.get("executionId"))
                    .sourceSystem((String) row.get("sourceSystem"))
                    .timestamp((String) row.get("startTime"))
                    .acknowledged(false)
                    .resolved(false)
                    .escalated(false)
                    .correlationId((String) row.get("correlationId"))
                    .affectedResources(List.of((String) row.get("jobName")))
                    .build());
        }

        for (ActiveJobDto job : activeJobs) {
            if (job.getStartTime() != null) {
                Instant start = Instant.parse(job.getStartTime());
                if (start.isBefore(Instant.now().minus(30, ChronoUnit.MINUTES))) {
                    alerts.add(AlertDto.builder()
                            .alertId("alert_long_" + job.getExecutionId())
                            .type("PERFORMANCE")
                            .severity("WARNING")
                            .title("Long Running Job: " + job.getJobName())
                            .description("Job has been running for over 30 minutes")
                            .jobExecutionId(job.getExecutionId())
                            .sourceSystem(job.getSourceSystem())
                            .timestamp(Instant.now().toString())
                            .acknowledged(false)
                            .resolved(false)
                            .escalated(false)
                            .correlationId(job.getCorrelationId())
                            .affectedResources(List.of(job.getJobName()))
                            .build());
                }
            }
        }

        return alerts;
    }

    private HistoricalMetricsDto buildTrends() {
        Instant now = Instant.now();
        Instant dayAgo = now.minus(24, ChronoUnit.HOURS);

        List<HistoricalMetricsDto.TrendPoint> executionTrends = new ArrayList<>();
        List<HistoricalMetricsDto.TrendPoint> throughputTrends = new ArrayList<>();
        List<HistoricalMetricsDto.TrendPoint> errorRateTrends = new ArrayList<>();

        List<HistoricalMetricsDto.TrendPoint[]> hourlyData = monitoringRepository.getHourlyTrends24h();
        for (HistoricalMetricsDto.TrendPoint[] points : hourlyData) {
            executionTrends.add(points[0]);
            throughputTrends.add(points[1]);
            errorRateTrends.add(points[2]);
        }

        return HistoricalMetricsDto.builder()
                .period("DAY")
                .startDate(dayAgo.toString())
                .endDate(now.toString())
                .jobExecutionTrends(executionTrends)
                .throughputTrends(throughputTrends)
                .errorRateTrends(errorRateTrends)
                .performanceScoreTrends(Collections.emptyList())
                .systemHealthTrends(Collections.emptyList())
                .build();
    }
}
