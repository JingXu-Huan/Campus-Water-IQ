package com.ncwu.repairservice.service.impl;


import com.alibaba.nacos.api.model.v2.Result;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.repairservice.entity.domain.IotDeviceEvent;
import com.ncwu.repairservice.mapper.IoTDeviceMapper;
import com.ncwu.repairservice.service.IoTDeviceService;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/1
 */
@Service
public class IoTDeviceServiceImpl extends ServiceImpl<IoTDeviceMapper, IotDeviceEvent> implements IoTDeviceService {

    @Override
    public Result<Boolean> addNewEvent() {
        return null;
    }
}
