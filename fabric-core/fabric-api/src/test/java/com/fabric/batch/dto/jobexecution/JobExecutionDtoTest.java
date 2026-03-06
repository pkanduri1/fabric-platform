package com.fabric.batch.dto.jobexecution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JobExecutionDtoTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void jobExecutionRequest_serialisesAndDeserialisesCorrectly() throws Exception {
        JobExecutionRequest req = JobExecutionRequest.builder()
                .jobConfigId("JC-1042")
                .sourceSystem("COLLECTIONS")
                .transformationRules(List.of("RULE_NORMALIZE_DATES"))
                .parameters(Map.of("inputFilePath", "/data/input/test.csv"))
                .callbackUrl("http://example.com/callback")
                .build();

        String json = mapper.writeValueAsString(req);
        JobExecutionRequest deserialized = mapper.readValue(json, JobExecutionRequest.class);

        assertThat(deserialized.getJobConfigId()).isEqualTo("JC-1042");
        assertThat(deserialized.getTransformationRules()).containsExactly("RULE_NORMALIZE_DATES");
    }

    @Test
    void jobStatusResponse_includesErrorDetailsWhenFailed() throws Exception {
        JobStatusResponse resp = JobStatusResponse.builder()
                .executionId("EXEC-8821")
                .status("FAILED")
                .errorDetails(JobStatusResponse.ErrorDetails.builder()
                        .errorCode("TRANSFORM_RULE_FAILURE")
                        .errorMessage("NPE at record 1042")
                        .build())
                .build();

        String json = mapper.writeValueAsString(resp);
        assertThat(json).contains("TRANSFORM_RULE_FAILURE");
    }
}
