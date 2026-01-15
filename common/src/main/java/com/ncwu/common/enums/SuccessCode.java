package com.ncwu.common.enums;


import lombok.Getter;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/27
 */
@Getter
public enum SuccessCode {
    DEVICE_OPEN_SUCCESS("DEV_1001", "设备开启成功"),
    DEVICE_STOP_SUCCESS("DEV_1002", "设备停止成功"),
    DEVICE_REGISTER_SUCCESS("DEV_1003", "设备注册成功"),
    DEVICE_RESET_SUCCESS("DEV_1004", "设备重置成功"),
    DEVICE_OFFLINE_SUCCESS("DEV_1005", "设备下线成功"),
    METER_CLOSE_SUCCESS("MTR_2001", "水表关阀成功"),
    METER_OPEN_SUCCESS("MTR_3001", "水表开阀成功"),
    METER_MODE_CHANGE_SUCCESS("MTR_4001","模拟模式切换成功"),

    TIME_CHANGE_SUCCESS("SYS_3001", "时间设置成功"),
    SEASON_CHANGE_SUCCESS("SYS_3002", "季节设置成功");


    final String code;
    final String message;

    SuccessCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
