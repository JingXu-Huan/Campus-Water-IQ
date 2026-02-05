package com.ncwu.repairservice.entity;


import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
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
    @Size(max = 64,message = "报修人姓名长度不能超过64个字符")
    String reportName;

    // 联系方式
    @Pattern(
            regexp = "^1[3-9]\\d{9}$",
            message = "请输入合法的中国大陆手机号"
    )
    private String contactInfo;

    //故障描述
    @Size(max = 500,message = "故障描述长度不能超过500个字符")
    String desc;

    //严重程度
    @Min(1)
    @Max(3)
    int severity;

    //状态
    @Pattern(regexp = "DRAFT|CONFIRMED|PROCESSING|DONE|CANCELLED", message = "status 只能是 " +
            "DRAFT、CONFIRMED、PROCESSING、DONE 或 CANCELLED")
    String status;

    //备注
    @Size(max = 255,message = "备注长度不能超过255个字符")
    String remark;
}
