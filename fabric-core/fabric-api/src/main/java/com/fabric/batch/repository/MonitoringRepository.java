package com.fabric.batch.repository;

import com.fabric.batch.dto.monitoring.ActiveJobDto;
import com.fabric.batch.dto.monitoring.HistoricalMetricsDto;

import java.util.List;
import java.util.Map;

public interface MonitoringRepository {

    List<ActiveJobDto> findActiveJobs(int limit);

    List<ActiveJobDto> findRecentCompletions(int limit);

    Map<String, Number> getExecutionStats24h();

    List<Map<String, Object>> findFailedExecutions24h(int limit);

    List<HistoricalMetricsDto.TrendPoint[]> getHourlyTrends24h();

    boolean isDatabaseHealthy();

    double getDatabaseResponseTimeMs();
}
