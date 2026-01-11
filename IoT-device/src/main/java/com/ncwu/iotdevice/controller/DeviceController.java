package com.ncwu.iotdevice.controller;


import com.ncwu.common.VO.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import com.ncwu.iotdevice.service.VirtualMeterDeviceService;
import com.ncwu.iotdevice.service.VirtualWaterQualityDeviceService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class DeviceController {

    private final VirtualMeterDeviceService virtualMeterDeviceService;
    private final VirtualWaterQualityDeviceService virtualWaterQualityDeviceService;

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
    public Result<String> start(@Min(1) @Max(33) @RequestParam(defaultValue = "1") int dormitoryBuildings,
                                @Min(1) @Max(30) @RequestParam(defaultValue = "1") int educationBuildings,
                                @Min(1) @Max(30) @RequestParam(defaultValue = "1") int experimentBuildings,
                                @Min(1) @Max(99) @RequestParam(defaultValue = "1") int floors,
                                @Min(1) @Max(999) @RequestParam(defaultValue = "10") int rooms) throws InterruptedException {
        int totalBuildings = dormitoryBuildings + educationBuildings + experimentBuildings;
        if (totalBuildings * floors * rooms > 100000) {
            throw new DeviceRegisterException("开启设备数量超过系统10万上限,请调整。");
        }
        return virtualMeterDeviceService.init(totalBuildings, floors, rooms,dormitoryBuildings,educationBuildings,experimentBuildings);
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


    /**
     * 查看某台设备当前运行状态
     *
     * @param ids 设备列表
     */
    @PostMapping("/status")
    public Result<Map<String, String>> checkDeviceStatus(@NotNull @NotEmpty @RequestBody List<@NotBlank String> ids) {

        return virtualMeterDeviceService.checkDeviceStatus(ids);
    }

    /**
     * 重置全部设备
     */
    @GetMapping("/destroyAll")
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
}
