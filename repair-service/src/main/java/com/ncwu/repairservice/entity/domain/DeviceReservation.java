package com.ncwu.repairservice.entity.domain;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
* 智能报修/预约表
*/
@Data
public class DeviceReservation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
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
