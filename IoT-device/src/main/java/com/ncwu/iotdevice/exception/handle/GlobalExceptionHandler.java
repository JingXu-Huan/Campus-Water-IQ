package com.ncwu.iotdevice.exception.handle;


import com.ncwu.common.VO.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
//        return Result.fail(e.getMessage(),ErrorCode.BUSINESS_INIT_ERROR.code()
//                ,ErrorCode.BUSINESS_INIT_ERROR.message());
//    }

}
