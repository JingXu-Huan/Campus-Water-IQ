package com.ncwu.iotdevice.controller;

import com.ncwu.common.domain.vo.Result;
import com.ncwu.common.domain.dto.IdsDTO;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.iotdevice.service.VirtualWaterQualityDeviceService;
import com.ncwu.iotdevice.utils.Utils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final Utils utils;
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
    public Result<String> startList(@RequestBody @Valid IdsDTO ids) {
        List<@NotBlank(message = "设备ID不能为空") @Pattern(
                regexp = "^[12][1-3](0[1-9]|[1-9][0-9])(0[1-9]|[1-9][0-9])(00[1-9]|0[1-9][0-9]|[1-9][0-9]{2})$",
                message = "设备ID格式错误"
        ) String> list = ids.getIds();
        if (utils.hasInvalidDevice("sensor", list)) {
            return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        return waterQualityDeviceService.startList(ids.getIds());
    }

    /**
     * 单设备或设备列表停止模拟任务
     *
     * @param ids 设备列表
     */
    @PostMapping("/stopList")
    public Result<String> stopList(@RequestBody @Valid IdsDTO ids) {
        List<@NotBlank(message = "设备ID不能为空") @Pattern(
                regexp = "^[12][1-3](0[1-9]|[1-9][0-9])(0[1-9]|[1-9][0-9])(00[1-9]|0[1-9][0-9]|[1-9][0-9]{2})$",
                message = "设备ID格式错误"
        ) String> list = ids.getIds();
        if (utils.hasInvalidDevice("sensor", list)) {
            return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        return waterQualityDeviceService.stopList(ids.getIds());
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
    public Result<String> offLineAll(@RequestBody @Valid IdsDTO ids) {
        List<@NotBlank(message = "设备ID不能为空") @Pattern(
                regexp = "^[12][1-3](0[1-9]|[1-9][0-9])(0[1-9]|[1-9][0-9])(00[1-9]|0[1-9][0-9]|[1-9][0-9]{2})$",
                message = "设备ID格式错误"
        ) String> list = ids.getIds();
        if (utils.hasInvalidDevice("sensor", list)) {
            return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        return waterQualityDeviceService.offLine(ids.getIds());
    }


}
