package com.ncwu.iotservice.controller;


import apis.BloomFilterService;
import cn.hutool.core.date.DateUtil;
import com.ncwu.common.dto.IdsDTO;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.vo.Result;
import com.ncwu.iotservice.service.IoTDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * IoT设备数据的控制层
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Slf4j
@RestController
@RequestMapping("/Data")
@RequiredArgsConstructor
public class IoTDataController {

    private final IoTDataService ioTDataService;

    @DubboReference(version = "1.0.0")
    private BloomFilterService bloomFilterService;

    /**
     * 得到某设备一段时间的用水量
     */
    @GetMapping("/Waterusage")
    public Result<Double> getRangeWaterUsed(@Min(1L) @RequestParam(value = "start") LocalDateTime start,
                                            @Min(1L) @RequestParam(value = "end") LocalDateTime end,
                                            @NotNull @NotBlank String deviceId) {
        if (bloomFilterService.mightContains(List.of(deviceId))) {
            LocalDateTime now = LocalDateTime.now();
            if (end.isBefore(start) || end.isAfter(now)) {
                return Result.fail("Data_1000", "传入时间非法");
            }
            try {
                DateUtil.date(start);
                DateUtil.date(end);
            } catch (Exception e) {
                return Result.fail("Data_1001", "传入时间非法");
            }
            return ioTDataService.getRangeUsage(start, end, deviceId);
        } else return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
    }

    @GetMapping("/schoolUsage")
    public Result<Double> getSchoolUsage(@Valid @Min(1) @Max(3) int school,
                                         @RequestParam(value = "start") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")LocalDateTime start,
                                         @RequestParam(value = "end") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        if (end.isBefore(start) || end.isAfter(now)) {
            return Result.fail("Data_1000", "传入时间非法");
        }
        try {
            DateUtil.date(start);
            DateUtil.date(end);
        } catch (Exception e) {
            return Result.fail("Data_1001", "传入时间非法");
        }
        return ioTDataService.getSchoolUsage(school,start,end);
    }

    /**
     * 得到设备列表的总用水量
     */
    @PostMapping("/sumWaterUsage")
    public Result<Map<String, Double>> getSumWaterUsage(@RequestBody @Valid IdsDTO ids) {
        return ioTDataService.getSumWaterUsage(ids.getIds());
    }

    /**
     * 得到某设备的总用水量
     */
    @GetMapping("/TotalWaterUsage")
    public Result<Double> getTotalUsage(String deviceId) {
        if (bloomFilterService.mightContains(List.of(deviceId))) {
            return ioTDataService.getTotalUsage(deviceId);
        } else return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
    }

    /**
     * 得到某设备的最近一条水流量
     */
    @GetMapping("/getFlowNow")
    public Result<Double> getFlow(String deviceId) {
        return ioTDataService.getFlowNow(deviceId);
    }
}
