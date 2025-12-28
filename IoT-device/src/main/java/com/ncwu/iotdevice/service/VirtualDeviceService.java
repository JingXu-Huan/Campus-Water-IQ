package com.ncwu.iotdevice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;

import java.util.List;
import java.util.Map;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */
public interface VirtualDeviceService extends IService<VirtualDevice> {
    Result<String> start();

    Result<String> startList(List<String> ids);

    Result<String> stopSimulation();

    Result<String> init(int buildings, int floors, int rooms) throws InterruptedException;

    String isRunning();

    String isInit();

    String getCurrentMode();

    Result<String> singleStopSimulation(List<String> ids);

    Result<Map<String, String>> checkDeviceStatus(List<String> ids);

    Result<String> changeTime(int time);
}
