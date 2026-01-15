package com.ncwu.repairservice.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 智能报修/预约表
 * </p>
 *
 * @author author
 * @since 2026-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("device_reservation")
public class DeviceReservation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备编码：ANBCXYZZZ
     */
    private String deviceCode;

    /**
     * A：1水表 / 2传感器
     */
    private Integer deviceType;

    /**
     * N：1花园 / 2龙子湖 / 3江淮
     */
    private Integer campusNo;

    /**
     * BC：01-99
     */
    private String buildingNo;

    /**
     * XY：01-99
     */
    private String floorNo;

    /**
     * ZZZ：001-999（传感器固定001）
     */
    private String unitNo;

    /**
     * 姓名
     */
    private String reporterName;

    /**
     * 联系方式
     */
    private String contactInfo;

    /**
     * 故障描述
     */
    private String faultDesc;

    /**
     * 严重程度：1低 2中 3高
     */
    private Integer severity;

    /**
     * 状态：DRAFT / CONFIRMED / PROCESSING / DONE / CANCELLED
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
