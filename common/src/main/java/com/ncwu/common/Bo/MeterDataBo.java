package com.ncwu.common.Bo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 水表设备上报数据载荷
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/23
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MeterDataBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    //设备类型
    Integer device;
    //设备编号
    String deviceId;
    //数据产生时间戳
    LocalDateTime timeStamp;
    //瞬时水流量
    Double flow;
    //总用水量
    Double totalUsage;
    //水压
    Double pressure;
    //水温
    Double waterTem;
    //水闸开闭状态
    String IsOpen;
    //数据状态
    String status;
}
