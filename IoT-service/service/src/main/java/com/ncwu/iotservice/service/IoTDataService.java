package com.ncwu.iotservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.common.vo.Result;
import com.ncwu.iotservice.entity.IotDeviceData;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
public interface IoTDataService extends IService<IotDeviceData> {
    Result<Double> getRangeUsage(long start, long end,String deviceId);

    Result<Double> getTotalUsage(String deviceId);
}
