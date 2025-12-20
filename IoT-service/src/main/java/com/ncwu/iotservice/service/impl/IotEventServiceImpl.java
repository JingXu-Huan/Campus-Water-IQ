package com.ncwu.iotservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.iotservice.entity.IotDeviceEvent;
import com.ncwu.iotservice.mapper.IoTDeviceEventMapper;
import com.ncwu.iotservice.service.IoTEventService;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Service
public class IotEventServiceImpl extends ServiceImpl<IoTDeviceEventMapper , IotDeviceEvent> implements IoTEventService {

}
