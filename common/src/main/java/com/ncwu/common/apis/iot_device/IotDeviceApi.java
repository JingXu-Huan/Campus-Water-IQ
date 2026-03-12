package com.ncwu.common.apis.iot_device;


import com.ncwu.common.domain.vo.Result;

import java.util.List;
import java.util.Map;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/12
 */
public interface IotDeviceApi {
    /**
     * 检查设备状态
     */
    Result<Map<String, String>> checkDeviceStatus(List<String> ids);


}
