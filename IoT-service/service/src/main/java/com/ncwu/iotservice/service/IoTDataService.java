package com.ncwu.iotservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.common.vo.Result;
import com.ncwu.iotservice.entity.IotDeviceData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
public interface IoTDataService extends IService<IotDeviceData> {
    Result<Double> getRangeUsage(LocalDateTime start, LocalDateTime end, String deviceId);

    Result<Double> getTotalUsage(String deviceId);

    Result<Map<String, Double>> getSumWaterUsage(@Valid @NotNull(message = "设备ID不能为空") @Size(min = 1, message = "设备ID不能为空") List<String> ids);

    Result<Double> getFlowNow(String deviceId);

    Result<Double> getSchoolUsage(int school, @Min(1L) LocalDateTime start, @Min(1L) LocalDateTime end);

    Result<Double> getAnnulus(String deviceId);

    Result<Double> getOfflineRate();

    Result<Double> getWaterQuality(String deviceId);

    Result<Double> getTurbidity(String deviceId, LocalDateTime time);

    Result<Double> getPh(String deviceId, LocalDateTime time);

    Result<Double> getChlorine(String deviceId, LocalDateTime time);

    Result<Map<LocalDateTime, Double>> getFlowTendency(LocalDateTime start, LocalDateTime end, String deviceId);
}
