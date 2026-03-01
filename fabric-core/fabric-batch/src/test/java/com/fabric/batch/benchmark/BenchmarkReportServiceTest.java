package com.fabric.batch.benchmark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BenchmarkReportServiceTest {

    private JdbcTemplate jdbcTemplate;
    private BenchmarkReportService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new BenchmarkReportService(jdbcTemplate);
    }

    @Test
    void savesReportViaJdbc() {
        var report = new BenchmarkReport(
            "run-001", Instant.now(), 100000, 480.0,
            Duration.ofMinutes(3), 1800_000_000L, 210.0, 0,
            List.of(), BenchmarkResult.PASS
        );

        service.save(report);

        verify(jdbcTemplate).update(
            anyString(),
            eq("run-001"), any(Instant.class), eq(100000L), eq(480.0),
            anyLong(), eq(1800_000_000L), eq(210.0), eq(0),
            eq("PASS")
        );
    }

    @Test
    void comparesAgainstBaseline() {
        var current = new BenchmarkReport(
            "run-002", Instant.now(), 100000, 480.0,
            Duration.ofMinutes(3), 1800_000_000L, 210.0, 0,
            List.of(), BenchmarkResult.PASS
        );

        // Mock baseline query
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class)))
            .thenReturn(100.0)   // baseline throughput
            .thenReturn(850.0)   // baseline avg chunk
            .thenReturn(2048_000_000L) // baseline peak mem
            .thenReturn(2);      // baseline errors

        String comparison = service.compareWithBaseline(current);
        assertThat(comparison).contains("Records/sec");
    }
}
