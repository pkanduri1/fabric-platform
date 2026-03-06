package com.fabric.batch.dto.jobexecution;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private String status;
    private String errorCode;
    private String message;
    private Instant timestamp;
    private List<FieldValidationError> errors;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldValidationError {
        private String field;
        private String message;
    }
}
