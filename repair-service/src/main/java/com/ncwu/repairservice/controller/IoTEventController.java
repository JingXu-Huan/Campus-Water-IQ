package com.ncwu.repairservice.controller;

import com.ncwu.common.domain.vo.Result;
import com.ncwu.repairservice.service.IoTDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 设备事件控制器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/1
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/iot-event")
public class IoTEventController {

    private final IoTDeviceService ioTDeviceService;

    /**
     * 添加用户的报修信息
     */
    @PostMapping("/addNewEvent")
    public com.alibaba.nacos.api.model.v2.Result<Boolean> addNewEvent() {
        return ioTDeviceService.addNewEvent();
    }

    /**
     * 手动清除告警
     */
    @DeleteMapping("/dissMissWarning")
    public Result<Boolean> dissMissWarn(@RequestBody List<String> ids) {
        return ioTDeviceService.dissMissWarning(ids);
    }

    /**
     * 得到系统所有告警数量
     */
    @GetMapping("/getAllWarningNum")
    public Result<Integer> getAllWarningNums() {
        return ioTDeviceService.getAllWarningsNum();
    }
}
