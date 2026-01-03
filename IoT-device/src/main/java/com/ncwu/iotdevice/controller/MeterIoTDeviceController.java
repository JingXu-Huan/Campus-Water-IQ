package com.ncwu.iotdevice.controller;

import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import com.ncwu.iotdevice.scheduling.ScheduledTasks;
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
    private final ScheduledTasks scheduledTasks;

    /**
     * 初始化
     * 建议前端传入参数，或者从配置文件读取默认值
     */
    @GetMapping("/init")
    public Result<String> start(@Min(1) @Max(99) @RequestParam(defaultValue = "1") int buildings,
                                @Min(1) @Max(99) @RequestParam(defaultValue = "1") int floors,
                                @Min(1) @Max(999) @RequestParam(defaultValue = "10") int rooms) throws InterruptedException {
        if (buildings * floors * rooms > 100000) {
            throw new DeviceRegisterException("开启设备数量超过系统10万上限,请调整。");
        }
        return virtualMeterDeviceService.init(buildings, floors, rooms);
    }

    /**
     * 开始模拟任务
     */
    @GetMapping("/startAll")
    public Result<String> startAllSimulator() {
        scheduledTasks.startTask();
        return virtualMeterDeviceService.start();
    }

    /**
     * 单设备或设备列表开始模拟任务
     */
    @PostMapping("/startList")
    public Result<String> startListSimulator(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        scheduledTasks.startTask();
        return virtualMeterDeviceService.startList(ids);
    }


    /**
     * 停止所有模拟任务
     */
    @GetMapping("/endAll")
    public Result<String> endAll() {
        scheduledTasks.stopTask();
        return virtualMeterDeviceService.stopSimulation();
    }

    /**
     * 单设备或设备列表停止模拟任务
     */
    @PostMapping("/endList")
    public Result<String> endList(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        return virtualMeterDeviceService.singleStopSimulation(ids);
    }

    /**
     * 单设备或设备列表下线
     */
    @PostMapping("/offLine")
    public Result<String> offline(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        return virtualMeterDeviceService.offline(ids);
    }

    /**
     * 单设备或多设备关闭阀门
     */
    @PostMapping("/closeTheValve")
    public Result<String> closeValue(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        return virtualMeterDeviceService.closeValue(ids);
    }

    /**
     * 单设备或多设备关闭阀门
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