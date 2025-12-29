package com.ncwu.iotdevice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import com.ncwu.iotdevice.service.VirtualWaterQualityDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualWaterQualityDeviceServiceImpl extends ServiceImpl<DeviceMapper, VirtualDevice>
        implements VirtualWaterQualityDeviceService {

}
