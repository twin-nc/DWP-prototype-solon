package com.netcompany.dcms.shared.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String correlationId,
        int status,
        String error,
        String message,
        Instant timestamp
) {
    public static ErrorResponse of(String correlationId, int status, String error, String message) {
        return new ErrorResponse(correlationId, status, error, message, Instant.now());
    }
}
