package com.ncwu.iotdevice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/29
 */
public interface VirtualWaterQualityDeviceService extends IService<VirtualDevice> {
    Result<String> startAll();

    Result<String> startList(@NotEmpty @NotNull List<String> ids);

    Result<String> stopAll();

    Result<String> offLine(@NotNull List<String> ids);

    Result<String> stopList(List<String> ids);

    Result<String> destroyAll();
}
