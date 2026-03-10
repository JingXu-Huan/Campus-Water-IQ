package com.ncwu.predictionservice.controller;


import com.ncwu.common.apis.IoTDataServiceApi;
import com.ncwu.common.apis.iot_service.IotDataService;
import com.ncwu.common.domain.bo.ToAIBO;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.predictionservice.service.AiService;
import com.ncwu.predictionservice.domain.vo.UsageVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/4
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AIServiceController {
    private final AiService aiService;

    @DubboReference(version = "1.0.0", interfaceClass = IoTDataServiceApi.class,timeout = 10000)
    private IoTDataServiceApi ioTDataServiceApi;

    @DubboReference(version = "1.0.0",interfaceClass = IotDataService.class,timeout = 10000)
    private IotDataService iotDataService;

    /**
     * 预测某校区明天的用水量（自动获取近七天数据）
     */
    @PostMapping("/predictTomorrowWaterUsage")
    public Result<UsageVO> predictTomorrowWaterUsage(@Min(1) @Max(3) @RequestParam int campus) {
        try {
            // 通过 Dubbo 调用 IoT-service 获取近七天的用水数据
            Result<ToAIBO> response = ioTDataServiceApi.getRecentWeekUsage();

            if (response == null || response.getData() == null) {
                return Result.fail(null, "获取用水数据失败");
            }

            ToAIBO toAIBO = response.getData();
            List<Double> usageData;

            // 根据校区获取对应的用水数据
            switch (campus) {
                case 1 -> usageData = toAIBO.getHY();
                case 2 -> usageData = toAIBO.getLH();
                case 3 -> usageData = toAIBO.getJH();
                default -> usageData = List.of();
            }

            if (usageData == null || usageData.isEmpty()) {
                return Result.fail(null, "暂无用水数据");
            }

            return aiService.predictTomorrowWaterUsage(usageData, campus);
        } catch (Exception e) {
            log.error("调用 IoT-service 失败: {}", e.getMessage(), e);
            return Result.fail(null, "获取用水数据失败: " + e.getMessage());
        }
    }

    /**
     * 评价水质，给出水质建议
     *
     * @param score 分数
     * @param ch    含氯量
     * @param th    浊度
     * @param ph    酸碱度
     */
    @PostMapping("/suggestionOfWater")
    public Result<String> suggestionOfWater(double score, double ph, double ch, double th) {
        return aiService.suggestionOfWater((int) score, ph, ch, th);
    }

    /**
     * 给出一条节水建议
     */
    @PostMapping("/suggestions")
    public Result<String> giveSuggestions() {
        return aiService.suggestionOfWaterUsage();
    }

    /**给出一条设备水质合格率的评价*/
    @GetMapping("/suggestionOfDevice")
    public Result<String> suggestionOfDevice(){
        Result<Double> qualityRate = iotDataService.getQualityRate();
        Double data = qualityRate.getData();
        return aiService.suggestionOfDevice(data);
    }


}
