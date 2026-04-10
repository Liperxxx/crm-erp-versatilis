package com.bustech.erp.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final Object errors;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message("Recurso criado com sucesso.")
            .data(data)
            .build();
    }

    public static ApiResponse<Void> noContent() {
        return ApiResponse.<Void>builder()
            .success(true)
            .message("Operação realizada com sucesso.")
            .build();
    }

    public static ApiResponse<Void> error(String message) {
        return ApiResponse.<Void>builder()
            .success(false)
            .message(message)
            .build();
    }

    public static ApiResponse<Void> error(String message, Object errors) {
        return ApiResponse.<Void>builder()
            .success(false)
            .message(message)
            .errors(errors)
            .build();
    }
}
