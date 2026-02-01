package com.ncwu.repairservice.entity.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;


/**
 * IoT 设备事件表
 */
@Data
public class IotDeviceEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private Long id;
    /**
     * 设备编号
     */
    private String deviceCode;
    /**
     * 设备类型
     */
    private String deviceType;
    /**
     * 事件类型（OFFLINE / ABNORMAL / THRESHOLD）
     */
    private String eventType;
    /**
     * 事件级别（INFO/WARN/ERROR）
     */
    private String eventLevel;

    private String eventDesc;

    private Date eventTime;

    private Integer handledFlag;

    private Date createTime;

    private Long parentId;

    private Integer cnt;

}
