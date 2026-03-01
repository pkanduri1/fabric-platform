package com.fabric.batch.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TaskExecutorConfig.class, TaskExecutorConfigTest.Config.class})
@TestPropertySource(properties = {
    "batch.performance.thread-pool.core-size=12",
    "batch.performance.thread-pool.max-size=24",
    "batch.performance.thread-pool.queue-capacity=300"
})
class TaskExecutorConfigTest {

    @EnableConfigurationProperties(BatchPerformanceProperties.class)
    static class Config {}

    @Autowired
    private TaskExecutor taskExecutor;

    @Test
    void usesPerformanceProperties() {
        assertThat(taskExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
        var pool = (ThreadPoolTaskExecutor) taskExecutor;
        assertThat(pool.getCorePoolSize()).isEqualTo(12);
        assertThat(pool.getMaxPoolSize()).isEqualTo(24);
    }
}
