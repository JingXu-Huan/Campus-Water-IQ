package com.ncwu.common.domain.dto;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/1
 */
@Data
public class IoTDeviceEventDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    String deviceCode;
    String eventTime;
    String deviceType;
    String eventLevel;
    String eventDesc;
    String eventType;
}
