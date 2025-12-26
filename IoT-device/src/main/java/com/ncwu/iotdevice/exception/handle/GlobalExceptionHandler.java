package com.ncwu.iotdevice.exception.handle;


import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
    public Result<String> handleValidationException(ConstraintViolationException e) {
        return Result.fail(400, "传递参数非法。楼宇数必须是[1,99]，层数必须是[1,99]，房间数必须是[1,999]。请检查");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DeviceRegisterException.class)
    public Result<String> tooManyDevicesException(DeviceRegisterException e) {
        return Result.fail(400, e.getMessage());
    }

}
