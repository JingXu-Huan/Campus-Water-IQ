package com.ncwu.common.enums;


import lombok.Getter;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/27
 */
@Getter
public enum SuccessCode {
    DEVICE_OPEN_SUCCESS("b0001","设备开启成功"),
    DEVICE_STOP_SUCCESS("b0000","设备停止成功"),
    DEVICE_REGISTER_SUCCESS("b0010","设备注册成功"),
    DEVICE_RESET_SUCCESS("b0100","设备重置成功"),
    TIME_CHANGE_SUCCESS("b0011","时间设置成功"),
    SEASON_CHANGE_SUCCESS("b0111","季节设置成功");

    final String code;
    final String message;

    SuccessCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
