package com.fabric.batch.dto.jobexecution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionRequest {

    @NotBlank(message = "jobConfigId is required")
    private String jobConfigId;

    @NotBlank(message = "sourceSystem is required")
    private String sourceSystem;

    @NotEmpty(message = "at least one transformationRule is required")
    private List<String> transformationRules;

    private Map<String, String> parameters;      // optional runtime overrides

    private String callbackUrl;                  // optional; must be a valid URL if present

    private Map<String, String> callbackHeaders; // optional; sent as-is with callback POST
}
