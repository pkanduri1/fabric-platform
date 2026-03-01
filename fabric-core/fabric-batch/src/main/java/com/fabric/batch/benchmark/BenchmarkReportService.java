package com.fabric.batch.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkReportService {

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = """
        INSERT INTO CM3INT.BATCH_BENCHMARK_RESULTS
        (RUN_ID, TIMESTAMP, TOTAL_RECORDS, THROUGHPUT_PER_SEC,
         DURATION_MS, PEAK_MEMORY_BYTES, AVG_CHUNK_TIME_MS, ERROR_COUNT, RESULT)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    public void save(BenchmarkReport report) {
        jdbcTemplate.update(INSERT_SQL,
            report.runId(), report.timestamp(), report.totalRecords(),
            report.throughputPerSecond(), report.totalDuration().toMillis(),
            report.peakMemoryBytes(), report.avgChunkTimeMs(),
            report.errorCount(), report.result().name()
        );
        log.info("Benchmark report saved: runId={} throughput={} records/sec result={}",
            report.runId(), report.throughputPerSecond(), report.result());
    }

    public String compareWithBaseline(BenchmarkReport current) {
        try {
            Double baselineThroughput = jdbcTemplate.queryForObject(
                "SELECT THROUGHPUT_PER_SEC FROM CM3INT.BATCH_BENCHMARK_RESULTS ORDER BY TIMESTAMP ASC FETCH FIRST 1 ROW ONLY",
                Double.class);
            Double baselineChunk = jdbcTemplate.queryForObject(
                "SELECT AVG_CHUNK_TIME_MS FROM CM3INT.BATCH_BENCHMARK_RESULTS ORDER BY TIMESTAMP ASC FETCH FIRST 1 ROW ONLY",
                Double.class);
            Long baselineMemory = jdbcTemplate.queryForObject(
                "SELECT PEAK_MEMORY_BYTES FROM CM3INT.BATCH_BENCHMARK_RESULTS ORDER BY TIMESTAMP ASC FETCH FIRST 1 ROW ONLY",
                Long.class);
            Integer baselineErrors = jdbcTemplate.queryForObject(
                "SELECT ERROR_COUNT FROM CM3INT.BATCH_BENCHMARK_RESULTS ORDER BY TIMESTAMP ASC FETCH FIRST 1 ROW ONLY",
                Integer.class);

            if (baselineThroughput == null) {
                return "No baseline found. Current run saved as baseline.";
            }

            var baseline = new BenchmarkReport(
                "baseline", Instant.EPOCH, current.totalRecords(),
                baselineThroughput, Duration.ZERO, baselineMemory,
                baselineChunk, baselineErrors, List.of(), BenchmarkResult.PASS
            );

            return BenchmarkReport.compare(baseline, current);
        } catch (Exception e) {
            log.warn("Could not load baseline for comparison: {}", e.getMessage());
            return "No baseline available. Current run will be saved as baseline.";
        }
    }
}
