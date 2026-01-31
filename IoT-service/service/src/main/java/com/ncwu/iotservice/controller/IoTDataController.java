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
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
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
        Result<Double> fail = checkDateAndDeviceId(start, end, deviceId);
        if (fail != null) return fail;
        else return ioTDataService.getRangeUsage(start, end, deviceId);
    }

    /**
     * 校验时间和 id 是否合法
     */
    private @Nullable Result<Double> checkDateAndDeviceId(LocalDateTime start, LocalDateTime end, String deviceId) {
        //校验时间
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
        //校验设备号
        if (!bloomFilterService.mightContains(List.of(deviceId))) {
            return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        return null;
    }

    /**
     * 校验时间是否合法
     */
    private Result<Double> checkDate(LocalDateTime start, LocalDateTime end) {
        //校验时间
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
        return null;
    }

    /**
     * 得到某校区的用水量
     */
    @GetMapping("/schoolUsage")
    public Result<Double> getSchoolUsage(@Valid @Min(1) @Max(3) int school,
                                         @RequestParam(value = "start") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                         LocalDateTime start,
                                         @RequestParam(value = "end") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                         LocalDateTime end) {
        Result<Double> fail = checkDate(start, end);
        if (fail != null) return fail;
        return ioTDataService.getSchoolUsage(school, start, end);
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
        } else
            return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
    }

    /**
     * 得到某设备的最近一条水流量
     */
    @GetMapping("/getFlowNow")
    public Result<Double> getFlow(String deviceId) {
        return ioTDataService.getFlowNow(deviceId);
    }

    /**返回时间段内的水流量数据*/
    @GetMapping("/getFlowTendency")
    public Result<Map<LocalDateTime,Double>> getFlowTendency(@RequestParam(value = "start") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                LocalDateTime start,
                                                @RequestParam(value = "end") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                LocalDateTime end,
                                                String deviceId) {
        Result<Double> fail = checkDate(start, end);
        if (fail!=null){
            return Result.fail(Collections.emptyMap(),ErrorCode.PARAM_VALIDATION_ERROR.code()
                    , ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        else return ioTDataService.getFlowTendency(start,end,deviceId);

    }

    /**
     * 得到某设备的环比增长率
     */
    @GetMapping("/getAnnulus")
    public Result<Double> getAnnulus(String deviceId) {
        if (bloomFilterService.mightContains(List.of(deviceId))) {
            return ioTDataService.getAnnulus(deviceId);
        } else return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
    }

    /**
     * 获得所有设备的离线率
     */
    @GetMapping("/getOffLineRate")
    public Result<Double> getOfflineRate() {
        return ioTDataService.getOfflineRate();
    }

    /*===============================================水质传感器=======================================================*/

    /**
     * 获取水质评分
     */
    @GetMapping("/getWaterQuality")
    public Result<Double> getWaterQuality(String deviceId) {
        return ioTDataService.getWaterQuality(deviceId);
    }

    /**
     * 获得浊度
     */
    @GetMapping("/getTurbidity")
    public Result<Double> getTurbidity(String deviceId) {
        return ioTDataService.getTurbidity(deviceId);
    }

    /**
     * 获得酸碱度
     */
    @GetMapping("/getPh")
    public Result<Double> getPh(String deviceId) {
        return ioTDataService.getPh(deviceId);
    }

    /**
     * 获得含氯量
     */
    @GetMapping("getChlorine")
    public Result<Double> getChlorine(String deviceId) {
        return ioTDataService.getChlorine(deviceId);
    }


}
