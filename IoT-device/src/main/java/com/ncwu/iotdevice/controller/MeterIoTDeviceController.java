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
import java.util.Map;

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

    /**
     * 查看某台设备当前运行状态
     */
    @PostMapping("/status")
    public Result<Map<String, String>> checkDeviceStatus(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {
        return virtualMeterDeviceService.checkDeviceStatus(ids);
    }

    /**
     * 更改当天的时间
     * <p>
     * 当然 😂 这不是逆转时间的公式，在物理世界，过去的人和事儿就是过去了
     * <p>
     * 这不过是虚拟世界罢了，祝你一切都好！
     *
     * @param time 你要重置的时间点
     * @author 景旭
     */
    @GetMapping("/timeChange")
    public Result<String> changeTime(@Min(0) @Max(24) int time) {
        return virtualMeterDeviceService.changeTime(time);
    }

    /**
     * 更改世界的季节
     * <p>
     * 我赋予了你重启四季的权力，
     * <p>
     * 却忘了提醒你，无论你将参数调回哪个季节，
     * <p>
     * 那些在枯叶中走散的人，都不会在花开时重逢。
     *
     * @param season - 你试图挽回的那个季节
     * @author 景旭
     */
    @GetMapping("/seasonChange")
    public Result<String> changeSeason(@Min(1) @Max(4) int season) {
        return virtualMeterDeviceService.changeSeason(season);
    }
}