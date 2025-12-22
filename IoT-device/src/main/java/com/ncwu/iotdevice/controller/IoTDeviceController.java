package com.ncwu.iotdevice.controller;


import com.ncwu.iotdevice.simulator.Test;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/22
 */
@Slf4j
@RestController("/test")
@RequiredArgsConstructor
public class IoTDeviceController {
    private final Test test;

    //启动定时任务
    @GetMapping("/start")
    public void start() throws InterruptedException {
        test.enable();
    }
    //停止定时任务
    @GetMapping("/end")
    public void end() throws InterruptedException{
        test.disable();
    }

}
