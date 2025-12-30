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
    Integer device;
    String deviceId;
    LocalDateTime timeStamp;
    Double ph;
    Double turbidity;
    Double chlorine;
    String status;
}
