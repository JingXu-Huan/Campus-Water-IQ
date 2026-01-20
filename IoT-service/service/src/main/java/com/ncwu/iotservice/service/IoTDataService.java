package com.ncwu.iotservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.common.vo.Result;
import com.ncwu.iotservice.entity.IotDeviceData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
public interface IoTDataService extends IService<IotDeviceData> {
    Result<Double> getRangeUsage(long start, long end,String deviceId);

    Result<Double> getTotalUsage(String deviceId);

    Result<Map<String, Double>> getSumWaterUsage(@Valid @NotNull(message = "设备ID不能为空") @Size(min = 1, message = "设备ID不能为空") List<String> ids);
}
