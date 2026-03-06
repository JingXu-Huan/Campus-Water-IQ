package com.ncwu.repairservice.service.impl;


import com.alibaba.nacos.api.model.v2.Result;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.repairservice.entity.domain.IotDeviceEvent;
import com.ncwu.repairservice.mapper.IoTDeviceMapper;
import com.ncwu.repairservice.service.IoTDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/1
 */
@Service
@RequiredArgsConstructor
public class IoTDeviceServiceImpl extends ServiceImpl<IoTDeviceMapper, IotDeviceEvent> implements IoTDeviceService {
    private final IoTDeviceMapper ioTDeviceMapper;

    @Override
    public Result<Boolean> addNewEvent() {
        return null;
    }

    @Override
    public com.ncwu.common.domain.vo.Result<Boolean> dissMissWarning(List<String> ids) {
        //这里清除的是系统内部检测到的的告警信息，并不是用户的报修单。
        //如果确定某些系统告警已经消除，则可调用此方法。
        ioTDeviceMapper.deleteByIds(ids);
        return com.ncwu.common.domain.vo.Result.ok(true);
    }
}
