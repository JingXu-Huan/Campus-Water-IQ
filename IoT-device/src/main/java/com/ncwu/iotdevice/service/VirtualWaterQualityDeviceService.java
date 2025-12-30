package com.ncwu.iotdevice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/29
 */
public interface VirtualWaterQualityDeviceService extends IService<VirtualDevice> {
    Result<String> startAll();
}
