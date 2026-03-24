package com.ncwu.common.apis.warning_service;
import com.ncwu.common.domain.IotDeviceEvent;
import com.ncwu.common.domain.vo.Result;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/24
 */

public interface EventInterFace {
    Result<Boolean> addNewEvent(IotDeviceEvent iotDeviceEvent);
}
