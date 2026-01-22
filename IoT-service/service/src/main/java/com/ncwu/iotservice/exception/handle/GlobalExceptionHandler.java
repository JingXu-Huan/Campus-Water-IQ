package com.ncwu.iotservice.exception.handle;

import com.ncwu.common.vo.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.iotservice.exception.QueryFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/20
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @Valid 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数校验失败: {}", message);
        return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), message);
    }

    /**
     * 处理 @Validated 参数校验异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数绑定失败: {}", message);
        return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), message);
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR.code(), ErrorCode.SYSTEM_ERROR.message());
    }

    @ExceptionHandler(QueryFailedException.class)
    public Result<?> handleQueryFailedException(QueryFailedException e) {
        String message = e.getMessage();
        return Result.fail(ErrorCode.QUERY_FAILED_ERROR.code(), message == null ?

                ErrorCode.QUERY_FAILED_ERROR.message() : message);
    }
}
