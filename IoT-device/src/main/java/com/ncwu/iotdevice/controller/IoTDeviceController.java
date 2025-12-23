package com.ncwu.iotdevice.controller;

import com.ncwu.iotdevice.simulator.WaterMeter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/simulator")
@RequiredArgsConstructor
public class IoTDeviceController {

    private final WaterMeter waterMeter;

    /**
     * 初始化并开始模拟
     * 建议前端传入参数，或者从配置文件读取默认值
     */
    @GetMapping("/start")
    public String start(@RequestParam(defaultValue = "1") int buildings,
                        @RequestParam(defaultValue = "1") int floors,
                        @RequestParam(defaultValue = "10") int rooms) {
        try {
            // 1. 如果还没初始化，先进行初始化（入库+入缓存）
            if (!waterMeter.isInit()) {
                waterMeter.init(buildings, floors, rooms);
            }
            // 2. 启动异步递归模拟流
            waterMeter.startSimulation();
            return "模拟任务启动成功！当前模式：" + waterMeter.getCurrentMode();
        } catch (Exception e) {
            return "启动失败：" + e.getMessage();
        }
    }

    /**
     * 停止模拟任务
     */
    @GetMapping("/end")
    public String end() {
        try {
            // 调用我们之前写的优雅停止方法
            waterMeter.stopSimulation();
            return "模拟任务已成功停止，所有调度已取消。";
        } catch (Exception e) {
            return "停止失败：" + e.getMessage();
        }
    }

    /**
     * 查看当前运行状态
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", waterMeter.isRunning());
        status.put("init", waterMeter.isInit());
        status.put("mode", waterMeter.getCurrentMode());
        return status;
    }
}