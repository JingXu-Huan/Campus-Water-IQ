package com.ncwu.iotdevice.controller;


import com.ncwu.common.annotation.RequireRole;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.common.domain.dto.IdsDTO;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import com.ncwu.iotdevice.service.VirtualMeterDeviceService;
import com.ncwu.iotdevice.service.VirtualWaterQualityDeviceService;
import com.ncwu.iotdevice.utils.Utils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 所有设备通用控制器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/3
 */
@Slf4j
@RestController
@RequestMapping("/device")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class DeviceController {

    private final VirtualMeterDeviceService virtualMeterDeviceService;
    private final VirtualWaterQualityDeviceService virtualWaterQualityDeviceService;
    private final Utils utils;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 初始化
     * 建议前端传入参数，或者从配置文件读取默认值
     *
     * @param rooms               每层房间数
     * @param floors              楼宇的层数
     * @param dormitoryBuildings  宿舍楼宇的数量
     * @param educationBuildings  教学区楼宇的数量
     * @param experimentBuildings 实验楼宇的数量
     */
    @GetMapping("/init")
    @RequireRole(value = {3}, names = {"管理员"})
    public Result<String> start(@Min(1) @Max(33) @RequestParam(defaultValue = "1") int dormitoryBuildings,
                                @Min(1) @Max(30) @RequestParam(defaultValue = "1") int educationBuildings,
                                @Min(1) @Max(30) @RequestParam(defaultValue = "1") int experimentBuildings,
                                @Min(1) @Max(99) @RequestParam(defaultValue = "1") int floors,
                                @Min(1) @Max(999) @RequestParam(defaultValue = "10") int rooms) throws InterruptedException {
        int totalBuildings = dormitoryBuildings + educationBuildings + experimentBuildings;
        if (3 * totalBuildings * floors * rooms > 100000) {
            throw new DeviceRegisterException("开启设备数量超过系统10万上限,请调整。");
        }
        return virtualMeterDeviceService.init(totalBuildings, floors, rooms, dormitoryBuildings, educationBuildings, experimentBuildings);
    }

    /**
     * 更改当天的时间
     * <p>
     * 当然 😂 这不是逆转时间的公式，在物理世界，过去的人和事儿就是过去了
     * <p>
     * 这不过是虚拟世界罢了，祝你一切都好！
     *
     * @param time 你要重置的时间点,以秒为单位
     * @author 景旭
     */
    @GetMapping("/timeChange")
    @RequireRole(value = {3}, names = {"管理员"})
    public Result<String> changeTime(@Min(0) @Max(86400) int time) {
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
    @RequireRole(value = {3}, names = {"管理员"})
    public Result<String> changeSeason(@Min(1) @Max(4) int season) {
        return virtualMeterDeviceService.changeSeason(season);
    }


    /**
     * 重置全部设备
     */
    @GetMapping("/destroyAll")
    @RequireRole(value = {3}, names = {"管理员"})
    public Result<String> destroyAllMeters() {
        Result<String> result = virtualWaterQualityDeviceService.destroyAll();
        Result<String> result1 = virtualMeterDeviceService.destroyAll();

        String code = result.getCode();
        String code1 = result1.getCode();
        if (Objects.equals(code, ErrorCode.DEVICE_CANT_RESET_ERROR.code()) ||
                Objects.equals(code1, ErrorCode.DEVICE_CANT_RESET_ERROR.code())) {
            return Result.fail(ErrorCode.DEVICE_CANT_RESET_ERROR.code(), ErrorCode.DEVICE_CANT_RESET_ERROR.message());
        } else {
            return Result.ok(SuccessCode.DEVICE_RESET_SUCCESS.getCode(), SuccessCode.DEVICE_RESET_SUCCESS.getMessage());
        }
    }

    /**
     * 查看某台设备当前运行状态
     *
     * @param ids 设备列表
     */
    @PostMapping("/status")
    public Result<Map<String, String>> checkDeviceStatus(@NotNull @RequestBody @Valid IdsDTO ids) {
        List<@NotBlank(message = "设备ID不能为空") @Pattern(
                regexp = "^[12][1-3](0[1-9]|[1-9][0-9])(0[1-9]|[1-9][0-9])(00[1-9]|0[1-9][0-9]|[1-9][0-9]{2})$",
                message = "设备ID格式错误"
        ) String> list = ids.getIds();
        if (utils.hasInvalidDevice(list)) {
            return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        return virtualMeterDeviceService.checkDeviceStatus(ids.getIds());
    }


    /**
     * 设置模拟模式 支持：leaking、burstPipe、normal三种模式
     */
    @GetMapping("/changeModel")
    @RequireRole(value = {3}, names = {"管理员"})
    public Result<String> changeModel(@NotNull @NotBlank String mode) {
        return virtualMeterDeviceService.changeMode(mode);
    }

    /**
     * 得到所有开启的设备数量
     */
    @GetMapping("/getDevicesNum")
    public Result<Integer> getNums() {
        return virtualMeterDeviceService.getDeviceNums();
    }

    /**
     * 检查设备是否已初始化
     */
    @GetMapping("/isInit")
    public Result<Boolean> isInit() {
        String isInit = stringRedisTemplate.opsForValue().get("isInit");
        return Result.ok("1".equals(isInit));
    }

    /**
     * 检查设备任务是否正在运行
     * 用于判断是否可以重置设备
     */
    @GetMapping("/taskStatus")
    public Result<TaskStatusVO> getTaskStatus() {
        String meterChecked = stringRedisTemplate.opsForValue().get("MeterChecked");
        String waterQualityChecked = stringRedisTemplate.opsForValue().get("WaterQualityChecked");

        TaskStatusVO status = new TaskStatusVO();
        status.setMeterRunning("1".equals(meterChecked));
        status.setSensorRunning("1".equals(waterQualityChecked));
        return Result.ok(status);
    }

    /**
     * 任务状态 VO
     */
    @Data
    public static class TaskStatusVO {
        private Boolean meterRunning;
        private Boolean sensorRunning;
    }

    /**
     * 获取楼宇配置信息
     * 从 Redis 中读取教学楼、实验楼的终止编号，计算宿舍楼终止编号
     */
    @GetMapping("/buildingConfig")
    public Result<BuildingConfigVO> getBuildingConfig() {
        try {
            // 从 Redis 获取配置
            String eduEndStr = stringRedisTemplate.opsForValue().get("device:educationBuildings");
            String expEndStr = stringRedisTemplate.opsForValue().get("device:experimentBuildings");
            String totalBuildings = stringRedisTemplate.opsForValue().get("TotalBuildings");
            String floorsStr = stringRedisTemplate.opsForValue().get("Floors");
            String roomsStr = stringRedisTemplate.opsForValue().get("Rooms");

            // 使用默认值
            int educationEnd = eduEndStr != null ? Integer.parseInt(eduEndStr) : 1;
            int experimentEnd = expEndStr != null ? Integer.parseInt(expEndStr) : 3;
            int floors = floorsStr != null ? Integer.parseInt(floorsStr) : 6;
            int rooms = roomsStr != null ? Integer.parseInt(roomsStr) : 10;

            // 计算宿舍楼起始编号 = Total - (experimentEnd + educationEnd) - 1
            assert totalBuildings != null;
            int dormitoryStart = Integer.parseInt(totalBuildings) - experimentEnd;

            BuildingConfigVO config = new BuildingConfigVO();
            config.setEducationStart(educationEnd);
            config.setExperimentStart(experimentEnd);
            config.setDormitoryStart(dormitoryStart);
            config.setTotalBuildings(Integer.parseInt(totalBuildings));
            config.setFloors(floors);
            config.setRooms(rooms);

            return Result.ok(config);
        } catch (Exception e) {
            log.error("获取楼宇配置失败", e);
            // 返回默认值
            BuildingConfigVO config = new BuildingConfigVO();
            config.setEducationStart(1);
            config.setExperimentStart(3);
            config.setDormitoryStart(4);
            config.setTotalBuildings(6);
            config.setFloors(6);
            config.setRooms(10);
            return Result.ok(config);
        }
    }

    /**
     * 得到当前的演示模式
     */
    @GetMapping("/getMode")
    public Result<String> getMode() {
        return Result.ok(stringRedisTemplate.opsForValue().get("mode"));
    }

    /**
     * 得到模拟季节
     */
    @GetMapping("/getSeason")
    public Result<Integer> getSeason() {
        String season = stringRedisTemplate.opsForValue().get("Season");
        return Result.ok(season == null ? 1 : Integer.parseInt(season));
    }

    /**
     * 楼宇配置 VO
     */
    @Data
    public static class BuildingConfigVO {
        private Integer educationStart;
        private Integer experimentStart;
        private Integer dormitoryStart;
        private Integer totalBuildings;
        private Integer floors;
        private Integer rooms;
    }
}
