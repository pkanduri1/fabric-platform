package com.fabric.batch.repository.impl;

import com.fabric.batch.dto.monitoring.ActiveJobDto;
import com.fabric.batch.dto.monitoring.HistoricalMetricsDto;
import com.fabric.batch.repository.MonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MonitoringRepositoryImpl implements MonitoringRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ActiveJobDto> findActiveJobs(int limit) {
        String sql = """
                SELECT EXECUTION_ID, JOB_NAME, e.STATUS, START_TIME,
                       RECORDS_PROCESSED, RECORDS_ERROR, CORRELATION_ID,
                       c.SOURCE_SYSTEM
                FROM MANUAL_JOB_EXECUTION e
                LEFT JOIN MANUAL_JOB_CONFIG c ON e.CONFIG_ID = c.CONFIG_ID
                WHERE e.STATUS IN ('STARTED', 'RUNNING')
                ORDER BY e.START_TIME DESC
                FETCH FIRST ? ROWS ONLY
                """;
        try {
            return jdbcTemplate.query(sql, new ActiveJobRowMapper(false), limit);
        } catch (Exception e) {
            log.warn("Failed to query active jobs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<ActiveJobDto> findRecentCompletions(int limit) {
        String sql = """
                SELECT EXECUTION_ID, JOB_NAME, e.STATUS, START_TIME, END_TIME,
                       RECORDS_PROCESSED, RECORDS_ERROR, CORRELATION_ID,
                       c.SOURCE_SYSTEM
                FROM MANUAL_JOB_EXECUTION e
                LEFT JOIN MANUAL_JOB_CONFIG c ON e.CONFIG_ID = c.CONFIG_ID
                WHERE e.STATUS IN ('COMPLETED', 'FAILED', 'CANCELLED')
                  AND e.END_TIME > (CURRENT_TIMESTAMP - INTERVAL '24' HOUR)
                ORDER BY e.END_TIME DESC
                FETCH FIRST ? ROWS ONLY
                """;
        try {
            return jdbcTemplate.query(sql, new ActiveJobRowMapper(true), limit);
        } catch (Exception e) {
            log.warn("Failed to query recent completions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Number> getExecutionStats24h() {
        String sql = """
                SELECT
                    COUNT(*) AS total,
                    SUM(CASE WHEN STATUS = 'COMPLETED' THEN 1 ELSE 0 END) AS success_count,
                    AVG(DURATION_SECONDS) AS avg_duration,
                    SUM(RECORDS_PROCESSED) AS total_records
                FROM MANUAL_JOB_EXECUTION
                WHERE START_TIME > (CURRENT_TIMESTAMP - INTERVAL '24' HOUR)
                """;
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(sql);
            Map<String, Number> result = new HashMap<>();
            result.put("total", row.get("TOTAL") != null ? ((Number) row.get("TOTAL")) : 0);
            result.put("successCount", row.get("SUCCESS_COUNT") != null ? ((Number) row.get("SUCCESS_COUNT")) : 0);
            result.put("avgDuration", row.get("AVG_DURATION") != null ? ((Number) row.get("AVG_DURATION")) : 0);
            result.put("totalRecords", row.get("TOTAL_RECORDS") != null ? ((Number) row.get("TOTAL_RECORDS")) : 0);
            return result;
        } catch (Exception e) {
            log.warn("Failed to compute execution stats: {}", e.getMessage());
            return Map.of("total", 0, "successCount", 0, "avgDuration", 0, "totalRecords", 0);
        }
    }

    @Override
    public List<Map<String, Object>> findFailedExecutions24h(int limit) {
        String sql = """
                SELECT EXECUTION_ID, JOB_NAME, ERROR_MESSAGE, START_TIME,
                       c.SOURCE_SYSTEM, CORRELATION_ID
                FROM MANUAL_JOB_EXECUTION e
                LEFT JOIN MANUAL_JOB_CONFIG c ON e.CONFIG_ID = c.CONFIG_ID
                WHERE e.STATUS = 'FAILED'
                  AND e.START_TIME > (CURRENT_TIMESTAMP - INTERVAL '24' HOUR)
                ORDER BY e.START_TIME DESC
                FETCH FIRST ? ROWS ONLY
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("executionId", rs.getString("EXECUTION_ID"));
                map.put("jobName", rs.getString("JOB_NAME"));
                map.put("errorMessage", rs.getString("ERROR_MESSAGE"));
                map.put("startTime", rs.getTimestamp("START_TIME").toInstant().toString());
                map.put("sourceSystem", rs.getString("SOURCE_SYSTEM"));
                map.put("correlationId", rs.getString("CORRELATION_ID"));
                return map;
            }, limit);
        } catch (Exception e) {
            log.warn("Failed to query failed executions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<HistoricalMetricsDto.TrendPoint[]> getHourlyTrends24h() {
        String sql = """
                SELECT TRUNC(START_TIME, 'HH24') AS hour_bucket,
                       COUNT(*) AS job_count,
                       SUM(NVL(RECORDS_PROCESSED, 0)) AS total_records,
                       SUM(CASE WHEN STATUS = 'FAILED' THEN 1 ELSE 0 END) AS failed_count
                FROM MANUAL_JOB_EXECUTION
                WHERE START_TIME > (CURRENT_TIMESTAMP - INTERVAL '24' HOUR)
                GROUP BY TRUNC(START_TIME, 'HH24')
                ORDER BY hour_bucket
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String ts = rs.getTimestamp("HOUR_BUCKET").toInstant().toString();
                int jobCount = rs.getInt("JOB_COUNT");
                long totalRecords = rs.getLong("TOTAL_RECORDS");
                int failedCount = rs.getInt("FAILED_COUNT");
                double errRate = jobCount > 0 ? (double) failedCount / jobCount * 100 : 0;

                return new HistoricalMetricsDto.TrendPoint[]{
                        HistoricalMetricsDto.TrendPoint.builder()
                                .timestamp(ts).value(jobCount).label("Jobs").build(),
                        HistoricalMetricsDto.TrendPoint.builder()
                                .timestamp(ts).value(totalRecords).label("Records").build(),
                        HistoricalMetricsDto.TrendPoint.builder()
                                .timestamp(ts).value(Math.round(errRate * 100.0) / 100.0).label("Error %").build()
                };
            });
        } catch (Exception e) {
            log.warn("Failed to compute hourly trends: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isDatabaseHealthy() {
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public double getDatabaseResponseTimeMs() {
        try {
            long start = System.currentTimeMillis();
            jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1;
        }
    }

    private static class ActiveJobRowMapper implements RowMapper<ActiveJobDto> {
        private final boolean completed;

        ActiveJobRowMapper(boolean completed) {
            this.completed = completed;
        }

        @Override
        public ActiveJobDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            long processed = rs.getLong("RECORDS_PROCESSED");
            long errors = rs.getLong("RECORDS_ERROR");

            ActiveJobDto.ActiveJobDtoBuilder builder = ActiveJobDto.builder()
                    .executionId(rs.getString("EXECUTION_ID"))
                    .jobName(rs.getString("JOB_NAME"))
                    .sourceSystem(rs.getString("SOURCE_SYSTEM"))
                    .status(rs.getString("STATUS"))
                    .priority("NORMAL")
                    .startTime(rs.getTimestamp("START_TIME").toInstant().toString())
                    .recordsProcessed(processed)
                    .errorCount((int) errors)
                    .warningCount(0)
                    .trendIndicator("STABLE")
                    .correlationId(rs.getString("CORRELATION_ID"));

            if (completed) {
                builder.progress(100)
                        .currentStage("Complete")
                        .throughputPerSecond(0)
                        .performanceScore(errors == 0 ? 100 : Math.max(50, 100 - (int) errors))
                        .lastHeartbeat(rs.getTimestamp("END_TIME") != null
                                ? rs.getTimestamp("END_TIME").toInstant().toString()
                                : Instant.now().toString());
            } else {
                long durationSec = Math.max(1,
                        (Instant.now().toEpochMilli() - rs.getTimestamp("START_TIME").toInstant().toEpochMilli()) / 1000);
                double throughput = processed > 0 ? (double) processed / durationSec : 0;

                builder.progress(0)
                        .currentStage("Processing")
                        .throughputPerSecond(Math.round(throughput * 100.0) / 100.0)
                        .performanceScore(errors == 0 ? 95 : Math.max(50, 95 - (int) errors))
                        .lastHeartbeat(Instant.now().toString());
            }

            return builder.build();
        }
    }
}
