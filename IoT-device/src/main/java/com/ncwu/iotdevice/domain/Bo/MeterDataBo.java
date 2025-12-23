package com.ncwu.iotdevice.domain.Bo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeterDataBo {
    Integer device;
    String deviceId;
    LocalDateTime timeStamp;
    Double flow;
    Double totalUsage;
    Double pressure;
    Double waterTem;
    String IsOpen;
    String status;
}
