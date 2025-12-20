package com.ncwu.iotservice.controller;


import com.ncwu.iotservice.service.IoTDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * IoT设备数据的控制层
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IoTDataController {

    private final IoTDataService ioTDataService;

}
