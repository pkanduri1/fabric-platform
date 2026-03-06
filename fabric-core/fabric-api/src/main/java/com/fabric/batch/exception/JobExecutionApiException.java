package com.fabric.batch.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class JobExecutionApiException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public JobExecutionApiException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static JobExecutionApiException notFound(String errorCode, String message) {
        return new JobExecutionApiException(errorCode, message, HttpStatus.NOT_FOUND);
    }

    public static JobExecutionApiException badRequest(String errorCode, String message) {
        return new JobExecutionApiException(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public static JobExecutionApiException conflict(String errorCode, String message) {
        return new JobExecutionApiException(errorCode, message, HttpStatus.CONFLICT);
    }
}
