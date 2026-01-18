package com.ncwu.iotservice.controller;


import cn.hutool.Hutool;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.ncwu.common.VO.Result;
import com.ncwu.iotservice.service.IoTDataService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/Waterusage")
    public Result<Integer> getRangeWaterUsed(@Min(1L) @RequestParam(value = "start") long start,
                                             @Min(1L)@RequestParam(value = "end") long end) {
        long now = System.currentTimeMillis();
        if (start >= now || end >= now) {
            return Result.fail("Data_1000", "传入时间非法");
        }
        try {
            DateUtil.date(start);
            DateUtil.date(end);
            return ioTDataService.getRangeUsage(start, end);
        } catch (Exception e) {
            return Result.fail("Data_1000", "传入时间非法");
        }
    }
}
