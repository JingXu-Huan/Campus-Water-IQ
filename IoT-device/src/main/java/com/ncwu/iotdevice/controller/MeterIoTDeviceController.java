package com.ncwu.iotdevice.controller;

import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.service.VirtualDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能水表的控制器
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/25
 */
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
    public Result<String> start(@RequestParam(defaultValue = "1") int buildings,
                                @RequestParam(defaultValue = "1") int floors,
                                @RequestParam(defaultValue = "10") int rooms) throws InterruptedException {
        return virtualDeviceService.init(buildings, floors, rooms);
    }
    /**开始模拟*/
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
    @GetMapping("/end")
    public Result<String> end() {
        // 调用我们之前写的优雅停止方法
        return virtualDeviceService.stopSimulation();
    }
    /**
     * 查看当前运行状态
     */
    @GetMapping("/status")
    public Map<String, String> status() {
        Map<String, String> status = new HashMap<>();
        status.put("running", virtualDeviceService.isRunning());
        status.put("init", virtualDeviceService.isInit());
        status.put("mode", virtualDeviceService.getCurrentMode());
        return status;
    }
}