package com.fabric.batch.guardrail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GuardrailPropertiesTest.Config.class)
@TestPropertySource(properties = {
    "batch.guardrails.memory-usage.threshold=85",
    "batch.guardrails.memory-usage.mode=WARN",
    "batch.guardrails.step-duration.threshold=600000",
    "batch.guardrails.step-duration.mode=FAIL"
})
class GuardrailPropertiesTest {

    @EnableConfigurationProperties(GuardrailProperties.class)
    static class Config {}

    @Autowired
    private GuardrailProperties props;

    @Test
    void bindsMemoryUsageGuardrail() {
        var mem = props.getMemoryUsage();
        assertThat(mem.getThreshold()).isEqualTo(85);
        assertThat(mem.getMode()).isEqualTo(GuardrailMode.WARN);
    }

    @Test
    void bindsStepDurationGuardrail() {
        var dur = props.getStepDuration();
        assertThat(dur.getThreshold()).isEqualTo(600000);
        assertThat(dur.getMode()).isEqualTo(GuardrailMode.FAIL);
    }
}
