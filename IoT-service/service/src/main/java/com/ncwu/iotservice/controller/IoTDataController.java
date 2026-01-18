package com.ncwu.iotservice.controller;


import apis.BloomFilterService;
import cn.hutool.core.date.DateUtil;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.vo.Result;
import com.ncwu.iotservice.service.IoTDataService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * 得到一段时间的用水量
     */
    @GetMapping("/Waterusage")
    public Result<Double> getRangeWaterUsed(@Min(1L) @RequestParam(value = "start") long start,
                                            @Min(1L) @RequestParam(value = "end") long end,
                                            @NotNull @NotBlank String deviceId) {
        if (bloomFilterService.mightContains(List.of(deviceId))) {
            long now = System.currentTimeMillis();
            if (start >= now || end >= now) {
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

    @GetMapping("/TotalWaterUsage")
    public Result<Double> getTotalUsage(String deviceId) {
        return ioTDataService.getTotalUsage(deviceId);
    }
}
