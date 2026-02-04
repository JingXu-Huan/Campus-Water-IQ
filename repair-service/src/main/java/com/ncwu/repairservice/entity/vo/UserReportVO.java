package com.ncwu.repairservice.entity.vo;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/4
 */
@Data
public class UserReportVO {
    //报修单ID
    String id;
    String deviceCode;
    String reportName;
    String contactInfo;
    //故障描述
    String desc;
    //严重程度
    int severity;
    //状态
    String status;
    //备注
    String remark;
    //创建时间
    LocalDateTime createdAt;
    //更新时间
    LocalDateTime updatedAt;
}
