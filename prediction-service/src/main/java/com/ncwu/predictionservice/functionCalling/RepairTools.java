package com.ncwu.predictionservice.functionCalling;


import com.ncwu.common.apis.repair_service.DeviceReservationServiceApi;
import com.ncwu.common.domain.dto.UserReportDTO;
import com.ncwu.common.domain.vo.Result;
import dev.langchain4j.agent.tool.Tool;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/13
 */
@Component
public class RepairTools {
    @DubboReference
    private DeviceReservationServiceApi deviceReservationServiceApi;

    @Tool("""
            此工具帮助用户填写设备报修单。需要以下信息：
            - deviceCode: 设备唯一编码
            - uid: 用户ID
            - reportName: 报修人姓名（最长64个字符）
            - contactInfo: 联系方式（中国大陆手机号格式：1[3-9]xxxxxxxxx）
            - desc: 故障描述（最长500个字符）
            - severity: 严重程度（1-3级，1为最低，3为最高）
            - status: 报修状态（可选值：DRAFT、CONFIRMED、PROCESSING、DONE、CANCELLED）
            - remark: 备注信息（最长255个字符）
            返回操作结果，成功返回true，失败返回false。
            """)
    Result<Boolean> addAReport(UserReportDTO userReportDTO){
        return deviceReservationServiceApi.addAReport(userReportDTO);
    }
}
