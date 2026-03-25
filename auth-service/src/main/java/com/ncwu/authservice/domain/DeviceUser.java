package com.ncwu.authservice.domain;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/25
 */
@Data
@TableName("device_user")
public class DeviceUser {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 设备ID
     */
    private String deviceCode;

    /**
     * 用户ID
     */
    private String uid;
}
