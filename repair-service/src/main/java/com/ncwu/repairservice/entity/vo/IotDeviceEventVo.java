package com.ncwu.repairservice.entity.vo;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/4
 */
@Data
public class IotDeviceEventVo {
    public long id;
    public String deviceCode;
    public String eventDesc;
    public String eventLevel;
    public String deviceType;
    LocalDateTime eventTime;
}
