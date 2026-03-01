package com.fabric.batch.guardrail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "batch.guardrails")
public class GuardrailProperties {

    private Threshold memoryUsage = new Threshold(85, GuardrailMode.WARN);
    private Threshold stepDuration = new Threshold(600000, GuardrailMode.FAIL);
    private Threshold chunkThroughput = new Threshold(50, GuardrailMode.WARN);
    private Threshold errorRate = new Threshold(5, GuardrailMode.FAIL);
    private Threshold connectionPoolSaturation = new Threshold(90, GuardrailMode.WARN);

    @Data
    public static class Threshold {
        private double threshold;
        private GuardrailMode mode;

        public Threshold() {
            this(0, GuardrailMode.WARN);
        }

        public Threshold(double threshold, GuardrailMode mode) {
            this.threshold = threshold;
            this.mode = mode;
        }
    }
}
