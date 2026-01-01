package com.ncwu.iotdevice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */
public interface VirtualMeterDeviceService extends IService<VirtualDevice> {
    Result<String> start();

    Result<String> startList(List<String> ids);

    Result<String> stopSimulation();

    Result<String> init(int buildings, int floors, int rooms) throws InterruptedException;

    Result<String> singleStopSimulation(List<String> ids);

    Result<Map<String, String>> checkDeviceStatus(List<String> ids);

    Result<String> changeTime(int time);

    Result<String> changeSeason(int season);

    Result<String> closeValue(@NotNull @NotEmpty List<String> ids);

    Result<String> open(@NotNull @NotEmpty List<String> ids);

    Result<String> openAllValue();

    Result<String> closeAllValue();

    Result<String> destroyAll();

}
