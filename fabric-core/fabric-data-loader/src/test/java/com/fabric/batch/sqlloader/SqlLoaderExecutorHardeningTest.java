package com.fabric.batch.sqlloader;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SqlLoaderExecutorHardeningTest {

    @Test
    void shouldFailFastWhenRejectThresholdExceeded() {
        SqlLoaderExecutor executor = new SqlLoaderExecutor();
        ReflectionTestUtils.setField(executor, "rejectPolicy", "FAIL_FAST");
        ReflectionTestUtils.setField(executor, "rejectMaxCount", 5L);
        ReflectionTestUtils.setField(executor, "rejectMaxPercent", 0.0);

        SqlLoaderResult result = new SqlLoaderResult();
        result.setSuccessful(true);
        result.setExecutionStatus("SUCCESS");
        result.setTotalRecords(100L);
        result.setRejectedRecords(10L);

        ReflectionTestUtils.invokeMethod(executor, "applyRejectThresholdPolicy", result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getExecutionStatus()).isEqualTo("FAILED_REJECT_THRESHOLD");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void shouldTolerateWhenPolicyIsTolerate() {
        SqlLoaderExecutor executor = new SqlLoaderExecutor();
        ReflectionTestUtils.setField(executor, "rejectPolicy", "TOLERATE");
        ReflectionTestUtils.setField(executor, "rejectMaxCount", 1L);
        ReflectionTestUtils.setField(executor, "rejectMaxPercent", 0.0);

        SqlLoaderResult result = new SqlLoaderResult();
        result.setSuccessful(true);
        result.setExecutionStatus("SUCCESS");
        result.setTotalRecords(10L);
        result.setRejectedRecords(2L);

        ReflectionTestUtils.invokeMethod(executor, "applyRejectThresholdPolicy", result);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getExecutionStatus()).isEqualTo("COMPLETED_WITH_REJECTIONS");
        assertThat(result.getWarnings()).isNotEmpty();
    }

    @Test
    void shouldCreateDeterministicArtifactPaths() throws Exception {
        SqlLoaderExecutor executor = new SqlLoaderExecutor();
        Path root = Files.createTempDirectory("sqlldr-artifacts");
        ReflectionTestUtils.setField(executor, "artifactRootDir", root.toString());

        SqlLoaderConfig cfg = SqlLoaderConfig.builder()
                .jobName("jobA")
                .correlationId("corr-123")
                .build();

        SqlLoaderResult result = new SqlLoaderResult();

        ReflectionTestUtils.invokeMethod(executor, "prepareArtifactPaths", cfg, result);

        assertThat(result.getLogFilePath()).contains("jobA_corr-123");
        assertThat(result.getBadFilePath()).contains("jobA_corr-123");
        assertThat(result.getDiscardFilePath()).contains("jobA_corr-123");
        assertThat(result.getAdditionalMetadata()).containsKey("artifactRunDir");

        Path runDir = Path.of(String.valueOf(result.getAdditionalMetadata().get("artifactRunDir")));
        assertThat(Files.exists(runDir)).isTrue();
    }
}
