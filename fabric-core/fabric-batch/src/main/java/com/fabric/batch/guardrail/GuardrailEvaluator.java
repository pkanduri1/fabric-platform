package com.fabric.batch.guardrail;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Evaluates performance guardrails after each step.
 * Uses annotation-compatible callbacks for forward-compat with Spring Batch 6 ChunkOrientedStep.
 */
@Slf4j
public class GuardrailEvaluator implements StepExecutionListener {

    private final GuardrailProperties props;
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    @Getter
    private final List<GuardrailViolation> violations = Collections.synchronizedList(new ArrayList<>());

    public GuardrailEvaluator(GuardrailProperties props) {
        this.props = props;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();

        checkMemoryUsage(stepName);
        checkStepDuration(stepExecution, stepName);
        checkErrorRate(stepExecution, stepName);

        return null; // don't override exit status unless FAIL
    }

    private void checkMemoryUsage(String stepName) {
        var heap = memoryBean.getHeapMemoryUsage();
        double usedPercent = (double) heap.getUsed() / heap.getMax() * 100;
        var guard = props.getMemoryUsage();

        if (usedPercent > guard.getThreshold()) {
            var violation = new GuardrailViolation(
                "memory-usage", guard.getThreshold(), usedPercent, guard.getMode(),
                stepName, -1, Instant.now(),
                String.format("Heap usage %.1f%% exceeds threshold %.1f%%", usedPercent, guard.getThreshold())
            );
            handleViolation(violation);
        }
    }

    private void checkStepDuration(StepExecution stepExecution, String stepName) {
        if (stepExecution.getStartTime() == null) return;
        long durationMs = java.time.Duration.between(stepExecution.getStartTime(), Instant.now()).toMillis();
        var guard = props.getStepDuration();

        if (durationMs > guard.getThreshold()) {
            var violation = new GuardrailViolation(
                "step-duration", guard.getThreshold(), durationMs, guard.getMode(),
                stepName, -1, Instant.now(),
                String.format("Step duration %dms exceeds threshold %.0fms", durationMs, guard.getThreshold())
            );
            handleViolation(violation);
        }
    }

    private void checkErrorRate(StepExecution stepExecution, String stepName) {
        long readCount = stepExecution.getReadCount();
        if (readCount == 0) return;

        long errorCount = stepExecution.getSkipCount() + stepExecution.getProcessSkipCount();
        double errorPercent = (double) errorCount / readCount * 100;
        var guard = props.getErrorRate();

        if (errorPercent > guard.getThreshold()) {
            var violation = new GuardrailViolation(
                "error-rate", guard.getThreshold(), errorPercent, guard.getMode(),
                stepName, -1, Instant.now(),
                String.format("Error rate %.1f%% exceeds threshold %.1f%%", errorPercent, guard.getThreshold())
            );
            handleViolation(violation);
        }
    }

    private void handleViolation(GuardrailViolation violation) {
        violations.add(violation);

        if (violation.mode() == GuardrailMode.WARN) {
            log.warn("GUARDRAIL [{}]: {}", violation.guardrailName(), violation.message());
        } else {
            log.error("GUARDRAIL FAIL [{}]: {}", violation.guardrailName(), violation.message());
            throw new GuardrailViolationException(violation);
        }
    }

    /** Reset violations between runs. */
    public void reset() {
        violations.clear();
    }
}
