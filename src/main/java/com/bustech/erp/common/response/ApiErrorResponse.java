package com.bustech.erp.common.response;

import com.bustech.erp.common.enums.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {

    public record FieldErrorDetail(String field, String message) {}

    public static ApiErrorResponse of(
            int status, String error, ErrorCode code, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, code.name(), message, path, null);
    }

    public static ApiErrorResponse of(
            int status, String error, ErrorCode code, String message, String path,
            List<FieldErrorDetail> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, code.name(), message, path, fieldErrors);
    }
}
