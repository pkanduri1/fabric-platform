package com.fabric.batch.service;

import com.fabric.batch.entity.ManualJobExecutionEntity;
import com.fabric.batch.repository.ManualJobExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookCallbackService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_MS = {0L, 5_000L, 25_000L};

    private final RestTemplate restTemplate;
    private final ManualJobExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Fire webhook callback for the given execution.
     * Called asynchronously after job completes or fails.
     */
    @Async
    public void fireCallback(String executionId) {
        Optional<ManualJobExecutionEntity> opt = executionRepository.findById(executionId);
        if (opt.isEmpty()) {
            log.warn("[{}] Cannot fire callback — execution not found", executionId);
            return;
        }
        ManualJobExecutionEntity entity = opt.get();

        if (entity.getCallbackUrl() == null || entity.getCallbackUrl().isBlank()) {
            log.debug("[{}] No callbackUrl — skipping webhook", executionId);
            executionRepository.updateCallbackStatus(executionId, "SKIPPED");
            return;
        }

        Map<String, Object> payload = buildPayload(entity);
        HttpHeaders headers = buildHeaders(entity.getCallbackHeaders());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (BACKOFF_MS[attempt] > 0) {
                sleep(BACKOFF_MS[attempt]);
            }
            try {
                ResponseEntity<String> resp = restTemplate.exchange(
                        entity.getCallbackUrl(), HttpMethod.POST, request, String.class);

                if (resp.getStatusCode().is2xxSuccessful()) {
                    log.info("[{}] Callback delivered on attempt {} — status={}",
                            executionId, attempt + 1, resp.getStatusCode());
                    executionRepository.updateCallbackStatus(executionId, "SENT");
                    return;
                }
                log.warn("[{}] Callback attempt {} returned non-2xx: {}",
                        executionId, attempt + 1, resp.getStatusCode());

            } catch (Exception ex) {
                log.warn("[{}] Callback attempt {} failed: {}",
                        executionId, attempt + 1, ex.getMessage());
            }
        }

        log.error("[{}] All {} callback attempts failed — marking FAILED", executionId, MAX_ATTEMPTS);
        executionRepository.updateCallbackStatus(executionId, "FAILED");
    }

    private Map<String, Object> buildPayload(ManualJobExecutionEntity e) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "FAILED".equals(e.getStatus()) ? "JOB_FAILED" : "JOB_COMPLETED");
        payload.put("executionId", e.getExecutionId());
        payload.put("jobConfigId", e.getConfigId());
        payload.put("sourceSystem", e.getTriggerSource());
        payload.put("status", e.getStatus());
        if ("FAILED".equals(e.getStatus())) {
            payload.put("errorCode", "TRANSFORM_RULE_FAILURE");
            payload.put("errorMessage", e.getErrorMessage());
            payload.put("retryUrl", "/api/v1/jobs/" + e.getExecutionId() + "/retry");
        }
        payload.put("auditTrailUrl", "/api/v1/jobs/" + e.getExecutionId() + "/audit");
        return payload;
    }

    @SuppressWarnings("unchecked")
    private HttpHeaders buildHeaders(String callbackHeadersJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (callbackHeadersJson != null && !callbackHeadersJson.isBlank()) {
            try {
                Map<String, String> extra = objectMapper.readValue(callbackHeadersJson, Map.class);
                extra.forEach(headers::set);
            } catch (Exception e) {
                log.warn("Failed to parse callbackHeaders JSON: {}", e.getMessage());
            }
        }
        return headers;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
