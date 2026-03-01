package com.fabric.batch.benchmark;

import com.fabric.batch.guardrail.GuardrailViolation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record BenchmarkReport(
    String runId,
    Instant timestamp,
    long totalRecords,
    double throughputPerSecond,
    Duration totalDuration,
    long peakMemoryBytes,
    double avgChunkTimeMs,
    int errorCount,
    List<GuardrailViolation> violations,
    BenchmarkResult result
) {

    public static String compare(BenchmarkReport baseline, BenchmarkReport current) {
        long baseMemMB = baseline.peakMemoryBytes() / (1024 * 1024);
        long currMemMB = current.peakMemoryBytes() / (1024 * 1024);

        double throughputDelta = ((current.throughputPerSecond() - baseline.throughputPerSecond())
            / baseline.throughputPerSecond()) * 100;
        double chunkDelta = ((current.avgChunkTimeMs() - baseline.avgChunkTimeMs())
            / baseline.avgChunkTimeMs()) * 100;
        double memDelta = ((double)(currMemMB - baseMemMB) / baseMemMB) * 100;

        BenchmarkResult verdict = current.throughputPerSecond() >= baseline.throughputPerSecond()
            && current.errorCount() <= baseline.errorCount()
            ? BenchmarkResult.PASS : BenchmarkResult.REGRESSION;

        return String.format("""
            Benchmark Report: %s
            ────────────────────────────────────────────────
            Metric             Baseline      Current     Delta
            Records/sec        %-13.1f %-11.1f %+.0f%%
            Avg chunk (ms)     %-13.1f %-11.1f %+.0f%%
            Peak memory (MB)   %-13d %-11d %+.0f%%
            Error count        %-13d %-11d %+d
            Guardrail hits     %-13d %-11d
            ────────────────────────────────────────────────
            Result: %s
            """,
            current.timestamp(),
            baseline.throughputPerSecond(), current.throughputPerSecond(), throughputDelta,
            baseline.avgChunkTimeMs(), current.avgChunkTimeMs(), chunkDelta,
            baseMemMB, currMemMB, memDelta,
            baseline.errorCount(), current.errorCount(), current.errorCount() - baseline.errorCount(),
            baseline.violations().size(), current.violations().size(),
            verdict
        );
    }
}
