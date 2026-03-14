package com.ncwu.iotservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.iotservice.entity.IotDeviceEvent;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
public interface IoTEventService extends IService<IotDeviceEvent> {
    Result<List<List<String>>> getLeakingDeviceList();
}
