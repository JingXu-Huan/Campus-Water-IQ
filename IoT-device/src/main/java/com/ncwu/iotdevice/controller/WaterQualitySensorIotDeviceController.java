package com.ncwu.iotdevice.controller;


import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.service.VirtualWaterQualityDeviceService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 水质传感器的控制器
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/25
 */
@Slf4j
@RestController()
@RequestMapping("/simulator/quality")
@RequiredArgsConstructor
public class WaterQualitySensorIotDeviceController {

    private final VirtualWaterQualityDeviceService waterQualityDeviceService;

    /**开启所有水质传感器*/
    @GetMapping("/startAll")
    public Result<String> startAll(){
       return waterQualityDeviceService.startAll();
    }

    /**单个或列表关闭水质传感器*/
    @PostMapping("/startList")
    public Result<String> startList(@NotEmpty @NotNull @RequestBody List<@NotBlank String> ids){
        return waterQualityDeviceService.startList(ids);
    }

    /**单个或列表开启水质传感器*/
    @PostMapping("/stopList")
    public Result<String> stopList(List<String> ids){
        return null;
    }

    /**关闭所有水质传感器*/
    @GetMapping("/stopAll")
    public Result<String> stopAll(){
        return null;
    }
}
