package com.ncwu.repairservice.entity;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/4
 */
@Data
public class UserReportDTO implements Serializable {
    @Serial
    private final static long serialVersionUID = 1L;
    //设备唯一编码
    String deviceCode;
    //报修人姓名
    String reportName;
    //联系方式
    String contactInfo;
    //故障描述
    String desc;
    //严重程度
    int severity;
    //状态
    String status;
    //备注
    String remark;
}
