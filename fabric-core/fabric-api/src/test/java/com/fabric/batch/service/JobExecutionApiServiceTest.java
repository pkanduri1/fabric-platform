package com.fabric.batch.service;

import com.fabric.batch.dto.jobexecution.*;
import com.fabric.batch.entity.ManualJobConfigEntity;
import com.fabric.batch.entity.ManualJobExecutionEntity;
import com.fabric.batch.exception.JobExecutionApiException;
import com.fabric.batch.repository.ManualJobConfigRepository;
import com.fabric.batch.repository.ManualJobExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobExecutionApiServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock ManualJobExecutionRepository executionRepository;
    @Mock ManualJobConfigRepository configRepository;

    @InjectMocks JobExecutionApiService service;

    private JobExecutionRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = JobExecutionRequest.builder()
                .jobConfigId("JC-1042")
                .sourceSystem("COLLECTIONS")
                .transformationRules(List.of("RULE_NORMALIZE_DATES"))
                .build();
    }

    // --- submitJob ---

    @Test
    void submitJob_validRequest_returnsSubmittedResponse() {
        // Given: config exists
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("JC-1042")))
                .thenReturn(1);
        when(executionRepository.save(any())).thenReturn(null);

        // When
        JobExecutionResponse resp = service.submitJob(validRequest, "file-watcher");

        // Then
        assertThat(resp.getStatus()).isEqualTo("STARTED");
        assertThat(resp.getExecutionId()).startsWith("EXEC-");
        assertThat(resp.getJobConfigId()).isEqualTo("JC-1042");
        assertThat(resp.getStatusUrl()).contains(resp.getExecutionId());
    }

    @Test
    void submitJob_unknownJobConfigId_throwsJobConfigNotFound() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("JC-9999")))
                .thenReturn(0);
        validRequest.setJobConfigId("JC-9999");

        assertThatThrownBy(() -> service.submitJob(validRequest, "file-watcher"))
                .isInstanceOf(JobExecutionApiException.class)
                .hasMessageContaining("JOB_CONFIG_NOT_FOUND");
    }

    // --- getStatus ---

    @Test
    void getStatus_existingId_returnsStatus() {
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId("EXEC-8821")
                .configId("JC-1042")
                .status("COMPLETED")
                .apiSource("API")
                .build();
        when(executionRepository.findById("EXEC-8821")).thenReturn(Optional.of(entity));

        JobStatusResponse resp = service.getStatus("EXEC-8821");

        assertThat(resp.getStatus()).isEqualTo("COMPLETED");
        assertThat(resp.getExecutionId()).isEqualTo("EXEC-8821");
    }

    @Test
    void getStatus_unknownId_throwsExecutionNotFound() {
        when(executionRepository.findById("EXEC-0000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatus("EXEC-0000"))
                .isInstanceOf(JobExecutionApiException.class)
                .hasMessageContaining("EXECUTION_NOT_FOUND");
    }

    // --- cancelJob ---

    @Test
    void cancelJob_submittedJob_cancelsSuccessfully() {
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId("EXEC-8821").status("STARTED").build();
        when(executionRepository.findById("EXEC-8821")).thenReturn(Optional.of(entity));

        JobExecutionResponse resp = service.cancelJob("EXEC-8821");

        assertThat(resp.getStatus()).isEqualTo("CANCELLED");
        verify(executionRepository).updateStatus("EXEC-8821", "CANCELLED");
    }

    @Test
    void cancelJob_completedJob_throwsInvalidStateTransition() {
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId("EXEC-8821").status("COMPLETED").build();
        when(executionRepository.findById("EXEC-8821")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.cancelJob("EXEC-8821"))
                .isInstanceOf(JobExecutionApiException.class)
                .hasMessageContaining("INVALID_STATE_TRANSITION");
    }

    // --- retryJob ---

    @Test
    void retryJob_failedJob_createsNewExecution() {
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId("EXEC-8821").status("FAILED").configId("JC-1042")
                .triggerSource("API").build();
        when(executionRepository.findById("EXEC-8821")).thenReturn(Optional.of(entity));
        when(executionRepository.save(any())).thenReturn(null);

        JobExecutionResponse resp = service.retryJob("EXEC-8821");

        assertThat(resp.getOriginalExecutionId()).isEqualTo("EXEC-8821");
        assertThat(resp.getNewExecutionId()).startsWith("EXEC-");
        assertThat(resp.getNewExecutionId()).isNotEqualTo("EXEC-8821");
    }

    @Test
    void retryJob_runningJob_throwsInvalidStateTransition() {
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId("EXEC-8821").status("RUNNING").build();
        when(executionRepository.findById("EXEC-8821")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.retryJob("EXEC-8821"))
                .isInstanceOf(JobExecutionApiException.class)
                .hasMessageContaining("INVALID_STATE_TRANSITION");
    }

    // --- listRecent ---

    @Test
    void listRecent_noFilters_returnsUpToDefaultLimit() {
        when(executionRepository.findRecentApiExecutions(isNull(), isNull(), isNull(), isNull(), eq(20)))
                .thenReturn(List.of());

        JobListResponse resp = service.listRecent(null, null, null, null, 20);

        assertThat(resp.getTotal()).isZero();
    }

    @Test
    void listRecent_limitExceeds100_capsAt100() {
        when(executionRepository.findRecentApiExecutions(any(), any(), any(), any(), eq(100)))
                .thenReturn(List.of());

        service.listRecent(null, null, null, null, 999);

        verify(executionRepository).findRecentApiExecutions(any(), any(), any(), any(), eq(100));
    }

    // --- runAllForSource ---

    @Test
    void runAllForSource_twoActiveConfigs_submitsBoth() {
        ManualJobConfigEntity c1 = new ManualJobConfigEntity();
        c1.setConfigId("JC-001"); c1.setSourceSystem("COLLECTIONS");

        ManualJobConfigEntity c2 = new ManualJobConfigEntity();
        c2.setConfigId("JC-002"); c2.setSourceSystem("COLLECTIONS");

        when(configRepository.findBySourceSystemAndStatus("COLLECTIONS", "ACTIVE"))
                .thenReturn(List.of(c1, c2));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1);
        when(executionRepository.save(any())).thenReturn(null);

        RunAllJobsResponse resp = service.runAllForSource("COLLECTIONS", "scheduler");

        assertThat(resp.getSubmittedCount()).isEqualTo(2);
        assertThat(resp.getExecutionIds()).hasSize(2);
        assertThat(resp.getSourceSystem()).isEqualTo("COLLECTIONS");
        assertThat(resp.getExecutionIds()).doesNotHaveDuplicates();
    }

    @Test
    void runAllForSource_noActiveConfigs_returnsZeroCount() {
        when(configRepository.findBySourceSystemAndStatus("EMPTY", "ACTIVE"))
                .thenReturn(List.of());

        RunAllJobsResponse resp = service.runAllForSource("EMPTY", "scheduler");

        assertThat(resp.getSubmittedCount()).isZero();
        assertThat(resp.getExecutionIds()).isEmpty();
    }

    // --- getAuditTrail ---

    @Test
    void getAuditTrail_existingExecution_returnsAuditEntries() {
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId("EXEC-8821")
                .configId("JC-1042")
                .status("COMPLETED")
                .executedBy("scheduler")
                .triggerSource("COLLECTIONS")
                .correlationId("corr-001")
                .startTime(java.time.LocalDateTime.now())
                .build();
        when(executionRepository.findById("EXEC-8821")).thenReturn(Optional.of(entity));

        JobAuditResponse resp = service.getAuditTrail("EXEC-8821");

        assertThat(resp.getExecutionId()).isEqualTo("EXEC-8821");
        assertThat(resp.getAuditEntries()).isNotEmpty();
        assertThat(resp.getAuditEntries().get(0).getActor()).isEqualTo("scheduler");
        assertThat(resp.getAuditEntries().get(0).getAction()).isEqualTo("SUBMITTED");
        assertThat(resp.getAuditEntries().get(0).getCorrelationId()).isEqualTo("corr-001");
        assertThat(resp.getAuditEntries().get(0).getDetails()).contains("COLLECTIONS");
    }

    @Test
    void getAuditTrail_unknownId_throwsExecutionNotFound() {
        when(executionRepository.findById("EXEC-0000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAuditTrail("EXEC-0000"))
                .isInstanceOf(JobExecutionApiException.class)
                .hasMessageContaining("EXECUTION_NOT_FOUND");
    }
}
