package com.ncwu.common.Bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 异常数据封装类
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/24
 */

@Data
public class ErrorDataMessageBO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    //设备类型
    String deviceType;
    //告警等级
    String level;
    //设备Id
    String deviceId;
    //原始数据载荷
    String payLoad;
    //错误信息描述
    String desc = "数据异常";
    //错误类型 ：OFFLINE ABNORMAL THRESHOLD
    String errorType;
    //建议操作
    String suggestion;
}
