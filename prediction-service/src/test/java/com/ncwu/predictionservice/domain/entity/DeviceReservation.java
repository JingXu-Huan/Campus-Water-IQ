package com.ncwu.predictionservice.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class DeviceReservation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备编码：ABCDXYZZZ */
    private String deviceCode;

    /** A：1水表 / 2传感器 */
    private Integer deviceType;

    /** B：1花园 / 2龙子湖 / 3江淮 */
    private Integer campusNo;

    /** CD：01-99 */
    private String buildingNo;

    /** XY：01-99 */
    private String floorNo;

    /** ZZZ：001-999（传感器固定001） */
    private String unitNo;

    /** 姓名 */
    private String reporterName;

    /** 联系方式 */
    private String contactInfo;

    /** 故障描述 */
    private String faultDesc;

    /** 严重程度：1低 2中 3高 */
    private Integer severity;

    /** 状态：DRAFT / CONFIRMED / PROCESSING / DONE / CANCELLED */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
