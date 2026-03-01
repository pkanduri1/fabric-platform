package com.fabric.batch.guardrail;

import java.time.Instant;

/**
 * Immutable record of a guardrail threshold breach.
 * Uses Java record for forward-compatibility with Spring Batch 6 immutable domain model.
 */
public record GuardrailViolation(
    String guardrailName,
    double threshold,
    double actualValue,
    GuardrailMode mode,
    String stepName,
    long chunkIndex,
    Instant timestamp,
    String message
) {}
