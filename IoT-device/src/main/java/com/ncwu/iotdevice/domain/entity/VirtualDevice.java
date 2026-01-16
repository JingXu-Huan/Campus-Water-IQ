package com.ncwu.iotdevice.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("virtual_device")
public class VirtualDevice {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 自增唯一主键

    @TableField("device_code")
    private String deviceCode; // 业务唯一编码 (ABCXYZZZ)

    @TableField("sn_code")
    private String snCode; // 设备物理序列号（出厂唯一码）

    @TableField("device_type")
    private Integer deviceType; // 1: 水表, 2: 传感器

    @TableField("campus_no")
    private String campusNo;//1:花园校区,2:龙子湖,3:江淮校区
    
    @TableField("building_no")
    private String buildingNo; // 楼宇编号（对应 BC）

    @TableField("floor_no")
    private String floorNo; // 楼层（对应 XY）

    @TableField("room_no")
    private String roomNo; // 房间/单元号（对应 ZZZ）

    @TableField("install_date")
    private Date installDate; // 安装日期

    @TableField("status")
    private String status; // 在线、离线、异常、报废

    @TableField("is_running")
    private Boolean isRunning; // 正在上报数据、停止上报数据
}
