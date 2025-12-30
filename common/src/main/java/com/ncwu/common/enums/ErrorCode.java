package com.ncwu.common.enums;


/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/27
 */
public enum ErrorCode {
    //关于参数失败的,均以A开头
    PARAM_VALIDATION_ERROR("A0400", "参数校验失败"),
    BUSINESS_ERROR("B0001", "业务异常"),
    BUSINESS_INIT_ERROR("B0001", "请先执行 init 进行设备初始化"),
    BUSINESS_DEVICE_RUNNING_NOW_ERROR("B0002", "模拟器已在运行中"),
    SYSTEM_ERROR("S0500", "系统异常"),
    UNKNOWN("S5000", "系统未知错误");


    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String message() {
        return defaultMessage;
    }
}

