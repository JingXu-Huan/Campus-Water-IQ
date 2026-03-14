package com.ncwu.iotservice.controller;

import com.ncwu.common.domain.vo.Result;
import com.ncwu.iotservice.service.IoTEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * IoT设备事件的控制层
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */

@Slf4j
@RestController
@RequiredArgsConstructor
public class IoTEventController {

    private final IoTEventService ioTEventService;

    /**
     * 获得可能存在漏水的设备编码
     */
    @PostMapping("/leakingDeviceList")
    public Result<List<List<String>>> getLeakingDeviceList() {
        return ioTEventService.getLeakingDeviceList();
    }

}
