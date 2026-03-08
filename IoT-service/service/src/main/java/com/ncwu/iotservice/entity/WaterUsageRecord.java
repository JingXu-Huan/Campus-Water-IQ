package com.ncwu.iotservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("water_usage_record")
public class WaterUsageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer school;

    @TableField("`usage`")
    private Double usage;

    private LocalDateTime recordDate;
}