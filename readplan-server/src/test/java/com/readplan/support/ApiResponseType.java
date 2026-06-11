package com.readplan.support;

import com.readplan.common.api.ApiResponse;

public record ApiResponseType<T>(int code, String message, T data) {

    public static <T> ApiResponseType<T> from(ApiResponse<T> response) {
        return new ApiResponseType<>(response.code(), response.message(), response.data());
    }
}
