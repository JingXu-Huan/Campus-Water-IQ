package com.ncwu.iotdevice.service;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */
@Service
public class VirtualDeviceServiceImpl extends ServiceImpl<DeviceMapper, VirtualDevice> implements VirtualDeviceService{
}
