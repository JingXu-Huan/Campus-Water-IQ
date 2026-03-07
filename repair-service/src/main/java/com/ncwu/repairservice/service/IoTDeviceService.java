package com.ncwu.repairservice.service;


import com.alibaba.nacos.api.model.v2.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.repairservice.entity.domain.IotDeviceEvent;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/1
 */
public interface IoTDeviceService extends IService<IotDeviceEvent> {
    Result<Boolean> addNewEvent();

    com.ncwu.common.domain.vo.Result<Boolean> dissMissWarning(List<String> ids);

    com.ncwu.common.domain.vo.Result<Integer> getAllWarningsNum();
}
