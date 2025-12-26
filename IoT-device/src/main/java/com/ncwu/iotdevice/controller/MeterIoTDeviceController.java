package com.ncwu.iotdevice.controller;

import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import com.ncwu.iotdevice.service.VirtualDeviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/simulator")
@RequiredArgsConstructor
public class MeterIoTDeviceController {

    private final VirtualDeviceService virtualDeviceService;

    /**
     * 初始化
     * 建议前端传入参数，或者从配置文件读取默认值
     */
    @GetMapping("/init")
    public Result<String> start(@Min(1) @Max(99) @RequestParam(defaultValue = "1") int buildings,
                                @Min(1) @Max(99) @RequestParam(defaultValue = "1") int floors,
                                @Min(1) @Max(999) @RequestParam(defaultValue = "10") int rooms) throws InterruptedException {
        if (buildings * floors * rooms > 10000) {
            throw new DeviceRegisterException("开启设备数量超过系统10万上限,请调整。");
        }
        return virtualDeviceService.init(buildings, floors, rooms);
    }

    /**
     * 开始模拟
     */
    @GetMapping("/startAll")
    public Result<String> startAllSimulator() {
        return virtualDeviceService.start();
    }

    @PostMapping("/startList")
    public Result<String> startListSimulator(@RequestBody List<String> ids) {
        return virtualDeviceService.startList(ids);
    }

    /**
     * 停止模拟任务
     */
    @GetMapping("/endAll")
    public Result<String> endAll() {
        return virtualDeviceService.stopSimulation();
    }

    /**
     * 单设备停止模拟任务
     */
    @PostMapping("/endList")
    public Result<String> endList(@RequestBody List<String> ids) {
        return virtualDeviceService.singleStopSimulation(ids);
    }

    /**
     * 查看某台设备当前运行状态
     */
    @PostMapping("/status")
    public Result<Map<String, String>> checkDeviceStatus(@RequestBody List<String> ids) {
        return virtualDeviceService.checkDeviceStatus(ids);

    }
}