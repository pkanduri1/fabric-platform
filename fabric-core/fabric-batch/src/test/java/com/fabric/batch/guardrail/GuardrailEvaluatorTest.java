package com.fabric.batch.guardrail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.StepExecution;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GuardrailEvaluatorTest {

    private GuardrailProperties props;
    private GuardrailEvaluator evaluator;

    @BeforeEach
    void setUp() {
        props = new GuardrailProperties();
        evaluator = new GuardrailEvaluator(props);
    }

    @Test
    void warnModeLogsButDoesNotThrow() {
        // memory-usage defaults to WARN mode
        props.getMemoryUsage().setThreshold(0); // 0% = always triggers
        props.getMemoryUsage().setMode(GuardrailMode.WARN);

        var stepExecution = mock(StepExecution.class);
        when(stepExecution.getStepName()).thenReturn("testStep");
        when(stepExecution.getReadCount()).thenReturn(100L);
        when(stepExecution.getWriteCount()).thenReturn(100L);

        // Should not throw
        evaluator.afterStep(stepExecution);

        List<GuardrailViolation> violations = evaluator.getViolations();
        assertThat(violations).anyMatch(v -> v.guardrailName().equals("memory-usage"));
    }

    @Test
    void failModeThrowsException() {
        props.getErrorRate().setThreshold(0); // 0% = always triggers
        props.getErrorRate().setMode(GuardrailMode.FAIL);

        var stepExecution = mock(StepExecution.class);
        when(stepExecution.getStepName()).thenReturn("testStep");
        when(stepExecution.getReadCount()).thenReturn(100L);
        when(stepExecution.getWriteCount()).thenReturn(50L);
        when(stepExecution.getSkipCount()).thenReturn(10L);
        when(stepExecution.getProcessSkipCount()).thenReturn(5L);

        assertThatThrownBy(() -> evaluator.afterStep(stepExecution))
            .isInstanceOf(GuardrailViolationException.class);
    }

    @Test
    void noViolationWhenWithinThresholds() {
        // Use high thresholds that won't trigger
        props.getMemoryUsage().setThreshold(99);
        props.getErrorRate().setThreshold(99);

        var stepExecution = mock(StepExecution.class);
        when(stepExecution.getStepName()).thenReturn("testStep");
        when(stepExecution.getReadCount()).thenReturn(100L);
        when(stepExecution.getWriteCount()).thenReturn(100L);
        when(stepExecution.getSkipCount()).thenReturn(0L);
        when(stepExecution.getProcessSkipCount()).thenReturn(0L);

        evaluator.afterStep(stepExecution);

        assertThat(evaluator.getViolations()).isEmpty();
    }
}
