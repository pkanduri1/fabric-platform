package com.fabric.batch.service;

import com.fabric.batch.entity.ManualJobExecutionEntity;
import com.fabric.batch.repository.ManualJobExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookCallbackServiceTest {

    @Mock RestTemplate restTemplate;
    @Mock ManualJobExecutionRepository executionRepository;

    @InjectMocks WebhookCallbackService service;

    @Test
    void fireCallback_noCallbackUrl_setsSkippedAndDoesNotCallHttp() {
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId("EXEC-0001")
                .callbackUrl(null)
                .status("COMPLETED")
                .build();
        when(executionRepository.findById("EXEC-0001")).thenReturn(Optional.of(entity));

        service.fireCallback("EXEC-0001");

        verify(restTemplate, never()).exchange(any(), any(), any(), eq(String.class));
        verify(executionRepository).updateCallbackStatus("EXEC-0001", "SKIPPED");
    }

    @Test
    void fireCallback_successOn2xx_setsSent() {
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId("EXEC-0002")
                .callbackUrl("http://example.com/cb")
                .status("COMPLETED")
                .configId("JC-1")
                .callbackHeaders(null)
                .build();
        when(executionRepository.findById("EXEC-0002")).thenReturn(Optional.of(entity));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        service.fireCallback("EXEC-0002");

        verify(executionRepository).updateCallbackStatus("EXEC-0002", "SENT");
    }

    @Test
    void fireCallback_allThreeAttemptsFail_setsFailed() {
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId("EXEC-0003")
                .callbackUrl("http://example.com/cb")
                .status("FAILED")
                .configId("JC-1")
                .callbackHeaders(null)
                .build();
        when(executionRepository.findById("EXEC-0003")).thenReturn(Optional.of(entity));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        service.fireCallback("EXEC-0003");

        // 3 attempts
        verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
        verify(executionRepository).updateCallbackStatus("EXEC-0003", "FAILED");
    }
}
