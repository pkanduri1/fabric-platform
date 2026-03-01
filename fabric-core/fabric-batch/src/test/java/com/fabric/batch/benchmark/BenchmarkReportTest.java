package com.fabric.batch.benchmark;

import com.fabric.batch.guardrail.GuardrailViolation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkReportTest {

    @Test
    void createsImmutableReport() {
        var report = new BenchmarkReport(
            "run-001", Instant.now(), 100000, 480.5,
            Duration.ofMinutes(3), 1800_000_000L, 210.0, 0,
            List.of(), BenchmarkResult.PASS
        );

        assertThat(report.runId()).isEqualTo("run-001");
        assertThat(report.totalRecords()).isEqualTo(100000);
        assertThat(report.throughputPerSecond()).isEqualTo(480.5);
        assertThat(report.errorCount()).isZero();
        assertThat(report.result()).isEqualTo(BenchmarkResult.PASS);
    }

    @Test
    void formatsComparisonReport() {
        var baseline = new BenchmarkReport(
            "baseline", Instant.now(), 100000, 100.0,
            Duration.ofMinutes(16), 2048_000_000L, 850.0, 2,
            List.of(), BenchmarkResult.PASS
        );
        var current = new BenchmarkReport(
            "current", Instant.now(), 100000, 480.0,
            Duration.ofMinutes(3), 1800_000_000L, 210.0, 1,
            List.of(), BenchmarkResult.PASS
        );

        String comparison = BenchmarkReport.compare(baseline, current);
        assertThat(comparison).contains("Records/sec");
        assertThat(comparison).contains("PASS");
    }
}
