package com.ncwu.iotdevice.exception.handle;

import com.ncwu.common.domain.vo.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

/**
 * 全局异常处理器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/22
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException ex) {

        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .toList();

        return Result.fail(String.join("; ", errors), ErrorCode.PARAM_VALIDATION_ERROR.code(),
                ErrorCode.PARAM_VALIDATION_ERROR.message());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DeviceRegisterException.class)
    public Result<String> tooManyDevicesException(DeviceRegisterException e) {
        return Result.fail(e.getMessage(), ErrorCode.DEVICE_ERROR.code()
                , ErrorCode.PARAM_VALIDATION_ERROR.message());
    }

    //    @ExceptionHandler(DeviceRegisterException)
//    public Result<String> deviceInitException(DeviceRegisterException e){
//        return Result.fail(e.getDesc(),ErrorCode.BUSINESS_INIT_ERROR.code()
//                ,ErrorCode.BUSINESS_INIT_ERROR.desc());
//    }
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public Result<?> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getAllErrors()
                .getFirst()
                .getDefaultMessage();
        return Result.fail("Sys0001", message);
    }
    
    @ExceptionHandler({BindException.class})
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getAllErrors()
                .getFirst()
                .getDefaultMessage();
        return Result.fail("Sys0002", message);
    }
    
    @ExceptionHandler({HandlerMethodValidationException.class})
    public Result<?> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String message = e.getAllValidationResults().stream()
                .findFirst()
                .map(result -> result.getResolvableErrors().stream()
                        .findFirst()
                        .map(MessageSourceResolvable::getDefaultMessage)
                        .orElse("参数验证失败"))
                .orElse("参数验证失败");
        return Result.fail("VALIDATION_ERROR", message);
    }
    
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "请求参数格式错误，请检查JSON格式";
        if (e.getMessage() != null && e.getMessage().contains("Cannot deserialize")) {
            message = "请求参数格式错误，期望对象格式但接收到数组格式";
        }
        return Result.fail("JSON_FORMAT_ERROR", message);
    }
    
//    @ExceptionHandler({Exception.class})
//    public Result<?> handleGeneralException(Exception e) {
//        // 记录异常信息用于调试
//        System.err.println("异常类型: " + e.getClass().getName());
//        System.err.println("异常信息: " + e.getDesc());
//        e.printStackTrace();
//
//        // 如果是验证相关的异常，尝试提取错误信息
//        if (e.getDesc() != null && e.getDesc().contains("Validation failure")) {
//            return Result.fail("VALIDATION_ERROR", "参数验证失败");
//        }
//
//        return Result.fail("Sys0003", e.getDesc());
//    }
}
