package com.ncwu.iotservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IoT 设备原始上报数据实体
 * 对应表：iot_device_data
 */
@Data
@TableName("iot_device_data")
public class IotDeviceData {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备唯一编号
     */
    private String deviceCode;

    /**
     * 设备类型（WATER_METER / WATER_QUALITY）
     */
    private String deviceType;

    /**
     * 设备采集时间
     */
    private LocalDateTime collectTime;

    /**
     * 设备上报原始数据（JSON 字符串）
     *
     * 注意：
     * 接入层不要反序列化，不要解析，不要业务绑定
     */
    private String dataPayload;

    /**
     * 数据入库时间
     */
    private LocalDateTime createTime;
}
