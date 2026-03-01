package com.fabric.batch.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BatchPerformancePropertiesTest.Config.class)
@TestPropertySource(properties = {
    "batch.performance.chunk-size=500",
    "batch.performance.grid-size=8",
    "batch.performance.thread-pool.core-size=8",
    "batch.performance.thread-pool.max-size=20",
    "batch.performance.thread-pool.queue-capacity=200",
    "batch.performance.sqlldr.parallel-degree=4",
    "batch.performance.sqlldr.bind-size=256000",
    "batch.performance.sqlldr.read-size=1048576"
})
class BatchPerformancePropertiesTest {

    @EnableConfigurationProperties(BatchPerformanceProperties.class)
    static class Config {}

    @Autowired
    private BatchPerformanceProperties props;

    @Test
    void bindsChunkAndGridSize() {
        assertThat(props.getChunkSize()).isEqualTo(500);
        assertThat(props.getGridSize()).isEqualTo(8);
    }

    @Test
    void bindsThreadPoolSettings() {
        assertThat(props.getThreadPool().getCoreSize()).isEqualTo(8);
        assertThat(props.getThreadPool().getMaxSize()).isEqualTo(20);
        assertThat(props.getThreadPool().getQueueCapacity()).isEqualTo(200);
    }

    @Test
    void bindsSqlldrSettings() {
        assertThat(props.getSqlldr().getParallelDegree()).isEqualTo(4);
        assertThat(props.getSqlldr().getBindSize()).isEqualTo(256000);
        assertThat(props.getSqlldr().getReadSize()).isEqualTo(1048576);
    }

    @Test
    void defaultsMatchCurrentBehavior() {
        // Test with a fresh instance (no properties bound)
        var defaults = new BatchPerformanceProperties();
        assertThat(defaults.getChunkSize()).isEqualTo(100);
        assertThat(defaults.getGridSize()).isEqualTo(4);
        assertThat(defaults.getThreadPool().getCoreSize()).isEqualTo(4);
        assertThat(defaults.getThreadPool().getMaxSize()).isEqualTo(10);
        assertThat(defaults.getThreadPool().getQueueCapacity()).isEqualTo(100);
        assertThat(defaults.getSqlldr().getParallelDegree()).isEqualTo(1);
    }
}
