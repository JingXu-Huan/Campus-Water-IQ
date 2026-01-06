package com.ncwu.iotdevice.controller;

import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.service.VirtualWaterQualityDeviceService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 水质传感器的控制器
 *
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

    /**
     * 开启所有水质传感器
     */
    @GetMapping("/startAll")
    public Result<String> startAll() {
        return waterQualityDeviceService.startAll();
    }

    /**
     * 单设备或设备列表开启模拟任务
     *
     * @param ids 设备列表
     */
    @PostMapping("/startList")
    public Result<String> startList(@NotNull @RequestBody List<@NotBlank String> ids) {
        return waterQualityDeviceService.startList(ids);
    }

    /**
     * 单设备或设备列表停止模拟任务
     *
     * @param ids 设备列表
     */
    @PostMapping("/stopList")
    public Result<String> stopList(@NotNull @RequestBody List<@NotBlank String> ids) {
        return waterQualityDeviceService.stopList(ids);
    }

    /**
     * 停止所有水质传感器上报任务
     */
    @GetMapping("/stopAll")
    public Result<String> stopAll() {
        return waterQualityDeviceService.stopAll();
    }

    /**
     * 单设备或设备列表下线
     *
     * @param ids 设备列表
     */
    @PostMapping("/stopAll")
    public Result<String> offLineAll(@NotNull @RequestBody List<@NotBlank String> ids) {
        return waterQualityDeviceService.offLine(ids);
    }
}
