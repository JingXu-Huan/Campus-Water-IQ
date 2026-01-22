package com.ncwu.common.Constants;


import lombok.Data;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/23
 */
@Data
public class DeviceStatus {
    public static final String NORMAL = "normal";
    public static final String CLOSE = "close";
    public static final String FAILURE = "failure";
    public static final String UNKNOWN_START_ALL_DEVICE = "未知错误-全部设备启动";
}
