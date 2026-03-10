package com.ncwu.predictionservice;

import com.ncwu.common.domain.vo.Result;
import com.ncwu.predictionservice.domain.vo.UsageVO;
import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/4
 */
public interface AiService {
    Result<UsageVO> predictTomorrowWaterUsage(List<Double> usage, int campus);

    Result<String> suggestionOfWaterUsage();

    Result<String> suggestionOfWater(int score, double ph, double ch, double th);

    Result<String> suggestionOfDevice(Double data);
}
