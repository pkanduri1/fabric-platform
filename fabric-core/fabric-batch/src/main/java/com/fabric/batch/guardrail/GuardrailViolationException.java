package com.fabric.batch.guardrail;

public class GuardrailViolationException extends RuntimeException {

    private final GuardrailViolation violation;

    public GuardrailViolationException(GuardrailViolation violation) {
        super(violation.message());
        this.violation = violation;
    }

    public GuardrailViolation getViolation() {
        return violation;
    }
}
