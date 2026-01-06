package com.ncwu.iotdevice.domain.Bo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 水质传感器信息载荷
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/30
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WaterQualityDataBo {
    //设备类型
    Integer device;
    //设备编号
    String deviceId;
    //数据产生时间
    LocalDateTime timeStamp;
    //酸碱
    Double ph;
    //浊度
    Double turbidity;
    //含氯量
    Double chlorine;
    //数据状态
    String status;
}
