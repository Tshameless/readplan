package com.readplan.common.handler;

import com.readplan.common.api.ApiResponse;
import com.readplan.common.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleBadRequest(Exception exception) {
        return ApiResponse.failure(400, "请求参数不合法");
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(SQLIntegrityConstraintViolationException exception) {
        return ApiResponse.failure(400, "重复操作，请勿重复提交");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknownException(Exception exception) {
        return ApiResponse.failure(500, "服务器内部异常");
    }
}
