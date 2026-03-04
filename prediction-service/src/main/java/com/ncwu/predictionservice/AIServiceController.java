package com.ncwu.predictionservice;


import com.ncwu.common.domain.vo.Result;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
    private final ChatLanguageModel chatLanguageModel;

    /**
     * 预测明天某校区的用水量
     */
    @PostMapping("/predictTomorrowWaterUsage")
    public Result<Double> predictTomorrowWaterUsage(@RequestBody List<Double> usage) {
        String res = chatLanguageModel.chat("给你一写用水量信息，预测下一个用水量信息,只返回一个浮点数即可" + usage.toString());
        Double v = Double.valueOf(res);
        return Result.ok(v);
    }

}
