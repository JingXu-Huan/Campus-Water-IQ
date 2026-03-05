package com.ncwu.predictionservice;


import com.ncwu.common.domain.vo.Result;
import com.ncwu.predictionservice.domain.vo.UsageVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 预测某校区明天的用水量
     */
    @PostMapping("/predictTomorrowWaterUsage")
    public Result<UsageVO> predictTomorrowWaterUsage(@RequestBody List<Double> usage, @Min(1) @Max(3) int campus) {
        return aiService.predictTomorrowWaterUsage(usage,campus);
    }



}
