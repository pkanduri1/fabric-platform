package com.fabric.batch.service;

import com.fabric.batch.mapping.YamlMappingService;
import com.fabric.batch.repository.BatchConfigurationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ManualBatchExecutionServiceStateTest {

    private ManualBatchExecutionService service;
    private Path stateDir;

    @BeforeEach
    void setup() throws IOException {
        BatchConfigurationRepository repo = Mockito.mock(BatchConfigurationRepository.class);
        JsonMappingService jsonMappingService = Mockito.mock(JsonMappingService.class);
        YamlMappingService yamlMappingService = Mockito.mock(YamlMappingService.class);
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        service = new ManualBatchExecutionService(repo, jsonMappingService, yamlMappingService, jdbcTemplate, objectMapper);

        stateDir = Files.createTempDirectory("batch-state-test");
        ReflectionTestUtils.setField(service, "executionStateDir", stateDir.toString());
        ReflectionTestUtils.setField(service, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(service, "retryBackoffMs", 1L);
        ReflectionTestUtils.setField(service, "checkpointInterval", 10);
    }

    @Test
    void checkpointRoundTripWorks() {
        String executionId = "exec-checkpoint-1";

        ReflectionTestUtils.invokeMethod(service, "writeCheckpoint", executionId, 250);
        Integer checkpoint = ReflectionTestUtils.invokeMethod(service, "readCheckpoint", executionId);

        assertEquals(250, checkpoint);

        ReflectionTestUtils.invokeMethod(service, "clearCheckpoint", executionId);
        Integer afterClear = ReflectionTestUtils.invokeMethod(service, "readCheckpoint", executionId);
        assertEquals(0, afterClear);
    }

    @Test
    void completionMarkerMakesExecutionIdempotent() {
        String executionId = "exec-done-1";
        String outputPath = "/tmp/output-idempotent.txt";

        ReflectionTestUtils.invokeMethod(service, "markCompleted", executionId, outputPath);

        Boolean done = ReflectionTestUtils.invokeMethod(service, "isAlreadyCompleted", executionId);
        String readPath = ReflectionTestUtils.invokeMethod(service, "getOutputPathFromCompletion", executionId);

        assertTrue(done);
        assertEquals(outputPath, readPath);
    }
}
