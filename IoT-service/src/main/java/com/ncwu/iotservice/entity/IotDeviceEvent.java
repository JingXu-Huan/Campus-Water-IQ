package com.ncwu.iotservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IoT 设备事件实体
 * 对应表：iot_device_event
 */
@Data
@TableName("iot_device_event")
public class IotDeviceEvent {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 设备类型（WATER_METER / WATER_QUALITY）
     */
    private String deviceType;

    /**
     * 事件类型（OFFLINE / ABNORMAL / THRESHOLD）
     */
    private String eventType;

    /**
     * 事件级别（INFO / WARN / ERROR）
     */
    private String eventLevel;

    /**
     * 事件描述
     */
    private String eventDesc;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 是否已处理（0-未处理，1-已处理）
     */
    private Integer handledFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
