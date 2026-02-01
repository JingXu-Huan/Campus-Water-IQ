package com.ncwu.repairservice.controller;



import com.alibaba.nacos.api.model.v2.Result;
import com.ncwu.repairservice.service.IoTDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Result<Boolean> addNewEvent() {
        return ioTDeviceService.addNewEvent();
    }
}
