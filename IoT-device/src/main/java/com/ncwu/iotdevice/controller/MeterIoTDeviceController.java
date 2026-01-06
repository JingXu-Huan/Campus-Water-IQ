package com.ncwu.iotdevice.controller;

import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.scheduling.MeterOnLineCheckerTasks;
import com.ncwu.iotdevice.service.VirtualMeterDeviceService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 智能水表的控制器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/25
 */
@Validated
@RestController
@RequestMapping("/simulator/meter")
@RequiredArgsConstructor
public class MeterIoTDeviceController {

    private final VirtualMeterDeviceService virtualMeterDeviceService;

    /**
     * 开启所有水表
     */
    @GetMapping("/startAll")
    public Result<String> startAllSimulator() {
        return virtualMeterDeviceService.start();
    }

    /**
     * 单设备或设备列表开始模拟任务
     */
    @PostMapping("/startList")
    public Result<String> startListSimulator(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        return virtualMeterDeviceService.startList(ids);
    }


    /**
     * 停止所有水表上报任务
     */
    @GetMapping("/endAll")
    public Result<String> endAll() {
        return virtualMeterDeviceService.stopSimulation();
    }

    /**
     * 单设备或设备列表停止模拟任务
     *
     * @param ids 设备列表
     */
    @PostMapping("/endList")
    public Result<String> endList(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        return virtualMeterDeviceService.singleStopSimulation(ids);
    }

    /**
     * 单设备或设备列表下线
     *
     * @param ids 设备列表
     */
    @PostMapping("/offLine")
    public Result<String> offline(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        return virtualMeterDeviceService.offline(ids);
    }

    /**
     * 单设备或设备列表关闭阀门
     *
     * @param ids 设备列表
     */
    @PostMapping("/closeTheValve")
    public Result<String> closeValue(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        return virtualMeterDeviceService.closeValue(ids);
    }

    /**
     * 单设备或设备列表开启阀门
     *
     * @param ids 设备列表
     */
    @PostMapping("/openTheValve")
    public Result<String> openValue(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        return virtualMeterDeviceService.open(ids);
    }

    /**
     * 所有设备开启阀门
     */
    @GetMapping("/openAllValues")
    public Result<String> openAllValues() {
        return virtualMeterDeviceService.openAllValue();
    }

    /**
     * 所有设备关闭阀门
     */
    @GetMapping("/closeAllValues")
    public Result<String> closeAllValues() {
        return virtualMeterDeviceService.closeAllValue();
    }

}