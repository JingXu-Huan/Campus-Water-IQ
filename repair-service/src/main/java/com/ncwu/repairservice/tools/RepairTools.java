package com.ncwu.repairservice.tools;

import com.ncwu.repairservice.entity.po.DeviceReservation;
import com.ncwu.repairservice.service.IDeviceReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RepairTools {
    private final IDeviceReservationService deviceReservationService;
    @Tool(description = "生成设备维修/预约单，并返回生成的预约单号")
    public String generateRepairReservation(

            @ToolParam(description = "设备编码，格式为 ANBCXYZZZ；若已提供则优先使用")
            String deviceCode,

            @ToolParam(description = "设备类型：1 水表 / 2 传感器；当 deviceCode 为空时必填")
            Integer deviceType,

            @ToolParam(description = "校区编号：1 花园 / 2 龙子湖 / 3 江淮；当 deviceCode 为空时必填")
            Integer campusNo,

            @ToolParam(description = "楼栋编号，两位字符串 01-99；当 deviceCode 为空时必填")
            String buildingNo,

            @ToolParam(description = "楼层编号，两位字符串 01-99；当 deviceCode 为空时必填")
            String floorNo,

            @ToolParam(description = "单元/房间编号，三位字符串 001-999；传感器固定为 001")
            String unitNo,

            @ToolParam(description = "报修人姓名")
            String reporterName,

            @ToolParam(description = "联系方式，优先手机号")
            String contactInfo,

            @ToolParam(description = "故障描述，包含现象、发生时间及是否影响使用")
            String faultDesc,

            @ToolParam(description = "严重程度：1 低 / 2 中 / 3 高")
            Integer severity,

            @ToolParam(description = "备注信息，可选")
            String remark
    ) {

        // === 1. deviceCode 处理逻辑 ===
        if (deviceCode == null || deviceCode.isBlank()) {
            // 兜底：根据字段生成
            deviceCode = String.format(
                    "%d%d%s%s%s",
                    deviceType,
                    campusNo,
                    buildingNo,
                    floorNo,
                    unitNo
            );
        }

        // === 2. 校验 deviceCode 格式 ===
        if (!deviceCode.matches("^[1-2][1-3][0-9]{2}[0-9]{2}[0-9]{3}$")) {
            throw new IllegalArgumentException("设备编号格式不正确，应为 ANBCXYZZZ");
        }

        // === 3. 构建实体 ===
        DeviceReservation reservation = new DeviceReservation()
                .setDeviceCode(deviceCode)
                .setDeviceType(deviceType)
                .setCampusNo(campusNo)
                .setBuildingNo(buildingNo)
                .setFloorNo(floorNo)
                .setUnitNo(unitNo)
                .setReporterName(reporterName)
                .setContactInfo(contactInfo)
                .setFaultDesc(faultDesc)
                .setSeverity(severity)
                .setRemark(remark)
                .setStatus("CONFIRMED")
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());

        // === 4. 持久化（示意） ===
         deviceReservationService.save(reservation);

        // === 5. 返回预约单号 ===
        return reservation.getDeviceCode();
    }

}
